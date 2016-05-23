/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.ImportPath;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KtPsiUtilTest extends KotlinTestWithEnvironment {
    @NotNull
    private KtFile loadPsiFile(@NotNull String name) {
        try {
            String text = KotlinTestUtils.doLoadFile(KotlinTestUtils.getTestDataPathBase(), name);
            return KotlinTestUtils.createFile(name + ".kt", text, getProject());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void testUnquotedIdentifier() {
        Assert.assertEquals("", KtPsiUtil.unquoteIdentifier(""));
        Assert.assertEquals("a2", KtPsiUtil.unquoteIdentifier("a2"));
        Assert.assertEquals("", KtPsiUtil.unquoteIdentifier("``"));
        Assert.assertEquals("a2", KtPsiUtil.unquoteIdentifier("`a2`"));
    }

    public void testUnquotedIdentifierOrFieldReference() {
        Assert.assertEquals("", KtPsiUtil.unquoteIdentifierOrFieldReference(""));
        Assert.assertEquals("a2", KtPsiUtil.unquoteIdentifierOrFieldReference("a2"));
        Assert.assertEquals("", KtPsiUtil.unquoteIdentifierOrFieldReference("``"));
        Assert.assertEquals("a2", KtPsiUtil.unquoteIdentifierOrFieldReference("`a2`"));
        Assert.assertEquals("$a2", KtPsiUtil.unquoteIdentifierOrFieldReference("$a2"));
        Assert.assertEquals("$a2", KtPsiUtil.unquoteIdentifierOrFieldReference("$`a2`"));
    }

    public void testConvertToImportPath() {
        Assert.assertEquals(null, getImportPathFromParsed("import "));
        Assert.assertEquals(new ImportPath(new FqName("some"), false), getImportPathFromParsed("import some."));
        Assert.assertEquals(null, getImportPathFromParsed("import *"));
        Assert.assertEquals(new ImportPath(new FqName("some.test"), true), getImportPathFromParsed("import some.test.* as SomeTest"));
        Assert.assertEquals(new ImportPath(new FqName("some"), false), getImportPathFromParsed("import some?.Test"));

        Assert.assertEquals(new ImportPath(new FqName("some"), false), getImportPathFromParsed("import some"));
        Assert.assertEquals(new ImportPath(new FqName("some"), true), getImportPathFromParsed("import some.*"));
        Assert.assertEquals(new ImportPath(new FqName("some.Test"), false), getImportPathFromParsed("import some.Test"));
        Assert.assertEquals(new ImportPath(new FqName("some.test"), true), getImportPathFromParsed("import some.test.*"));
        Assert.assertEquals(new ImportPath(new FqName("some.test"), false, Name.identifier("SomeTest")), getImportPathFromParsed("import some.test as SomeTest"));

        Assert.assertEquals(new ImportPath(new FqName("some.Test"), false), getImportPathFromParsed("import some./* hello world */Test"));
        Assert.assertEquals(new ImportPath(new FqName("some.Test"), false), getImportPathFromParsed("import some.    Test"));

        Assert.assertNotSame(new ImportPath(new FqName("some.test"), false), getImportPathFromParsed("import some.Test"));
    }

    public void testIsLocalClass() throws IOException {
        String text = FileUtil.loadFile(new File(KotlinTestUtils.getTestDataPathBase() + "/psiUtil/isLocalClass.kt"), true);
        KtClass aClass = KtPsiFactoryKt.KtPsiFactory(getProject()).createClass(text);

        @SuppressWarnings("unchecked")
        Collection<KtClassOrObject> classOrObjects = PsiTreeUtil.collectElementsOfType(aClass, KtClassOrObject.class);

        for (KtClassOrObject classOrObject : classOrObjects) {
            String classOrObjectName = classOrObject.getName();
            if (classOrObjectName != null && classOrObjectName.contains("Local")) {
                assertTrue("KtPsiUtil.isLocal should return true for " + classOrObjectName, KtPsiUtil.isLocal(classOrObject));
            }
            else {
                assertFalse("KtPsiUtil.isLocal should return false for " + classOrObjectName, KtPsiUtil.isLocal(classOrObject));
            }
        }
    }

    public void testIsSelectorInExpression() {
        checkIsSelectorInQualified();
    }

    public void testIsSelectorInType() {
        checkIsSelectorInQualified();
    }

    @Override
    protected KotlinCoreEnvironment createEnvironment() {
        return KotlinCoreEnvironment.createForTests(
                getTestRootDisposable(), KotlinTestUtils.newConfiguration(), EnvironmentConfigFiles.JVM_CONFIG_FILES
        );
    }

    private ImportPath getImportPathFromParsed(String text) {
        KtImportDirective importDirective =
                PsiTreeUtil.findChildOfType(KtPsiFactoryKt.KtPsiFactory(getProject()).createFile(text), KtImportDirective.class);

        assertNotNull("At least one import directive is expected", importDirective);

        return importDirective.getImportPath();
    }

    private void checkIsSelectorInQualified() {
        String trueResultString = "/*true*/";
        String falseResultString = "/*false*/";

        KtFile file = loadPsiFile(new File("psiUtil/" + getTestName(true) + ".kt").getPath());
        String text = file.getText();

        // /*true*/|/*false*/
        Pattern pattern = Pattern.compile(String.format("%s|%s", Pattern.quote(trueResultString), Pattern.quote(falseResultString)));
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            boolean expected = trueResultString.equals(matcher.group());
            int offset = matcher.end();

            KtSimpleNameExpression expression = PsiTreeUtil.findElementOfClassAtOffset(file, offset, KtSimpleNameExpression.class, true);

            String modifiedWithOffset = new StringBuilder(text).insert(offset, "<======caret======>").toString();

            Assert.assertNotNull("Can't find expression in text:\n" + modifiedWithOffset, expression);
            Assert.assertSame(expected + " result was expected at\n" + modifiedWithOffset,
                              expected, KtPsiUtil.isSelectorInQualified(expression));
        }
    }
}
