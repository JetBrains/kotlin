/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.psi;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.util.PsiTreeUtil;
import junit.framework.Assert;
import org.jetbrains.jet.JetLiteFixture;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

public class JetPsiUtilTest extends JetLiteFixture {
    public void testUnquotedIdentifier() {
        Assert.assertEquals("", JetPsiUtil.unquoteIdentifier(""));
        Assert.assertEquals("a2", JetPsiUtil.unquoteIdentifier("a2"));
        Assert.assertEquals("", JetPsiUtil.unquoteIdentifier("``"));
        Assert.assertEquals("a2", JetPsiUtil.unquoteIdentifier("`a2`"));
    }

    public void testUnquotedIdentifierOrFieldReference() {
        Assert.assertEquals("", JetPsiUtil.unquoteIdentifierOrFieldReference(""));
        Assert.assertEquals("a2", JetPsiUtil.unquoteIdentifierOrFieldReference("a2"));
        Assert.assertEquals("", JetPsiUtil.unquoteIdentifierOrFieldReference("``"));
        Assert.assertEquals("a2", JetPsiUtil.unquoteIdentifierOrFieldReference("`a2`"));
        Assert.assertEquals("$a2", JetPsiUtil.unquoteIdentifierOrFieldReference("$a2"));
        Assert.assertEquals("$a2", JetPsiUtil.unquoteIdentifierOrFieldReference("$`a2`"));
    }

    public void testConvertToImportPath() {
        Assert.assertEquals(null, getImportPathFromParsed("import "));
        Assert.assertEquals(null, getImportPathFromParsed("import some."));
        Assert.assertEquals(null, getImportPathFromParsed("import *"));
        Assert.assertEquals(null, getImportPathFromParsed("import some.test.* as SomeTest"));
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
        String text = FileUtil.loadFile(new File(getTestDataPath() + "/psiUtil/isLocalClass.kt"), true);
        JetClass aClass = JetPsiFactory(getProject()).createClass(text);

        @SuppressWarnings("unchecked")
        Collection<JetClassOrObject> classOrObjects = PsiTreeUtil.collectElementsOfType(aClass, JetClassOrObject.class);

        for (JetClassOrObject classOrObject : classOrObjects) {
            String classOrObjectName = classOrObject.getName();
            if (classOrObjectName != null && classOrObjectName.contains("Local")) {
                assertTrue("JetPsiUtil.isLocalClass should return true for " + classOrObjectName, JetPsiUtil.isLocal(classOrObject));
            }
            else {
                assertFalse("JetPsiUtil.isLocalClass should return false for " + classOrObjectName, JetPsiUtil.isLocal(classOrObject));
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
    protected JetCoreEnvironment createEnvironment() {
        return JetCoreEnvironment.createForTests(getTestRootDisposable(), new CompilerConfiguration());
    }

    private ImportPath getImportPathFromParsed(String text) {
        JetImportDirective importDirective =
                PsiTreeUtil.findChildOfType(JetPsiFactory(getProject()).createFile(text), JetImportDirective.class);

        assertNotNull("At least one import directive is expected", importDirective);

        return importDirective.getImportPath();
    }

    private void checkIsSelectorInQualified() {
        String trueResultString = "/*true*/";
        String falseResultString = "/*false*/";

        JetFile file = loadPsiFile(new File("psiUtil/" + getTestName(true) + ".kt").getPath());
        String text = file.getText();

        // /*true*/|/*false*/
        Pattern pattern = Pattern.compile(String.format("%s|%s", Pattern.quote(trueResultString), Pattern.quote(falseResultString)));
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            boolean expected = trueResultString.equals(matcher.group());
            int offset = matcher.end();

            JetSimpleNameExpression expression = PsiTreeUtil.findElementOfClassAtOffset(file, offset, JetSimpleNameExpression.class, true);

            String modifiedWithOffset = new StringBuilder(text).insert(offset, "<======caret======>").toString();

            Assert.assertNotNull("Can't find expression in text:\n" + modifiedWithOffset, expression);
            Assert.assertSame(expected + " result was expected at\n" + modifiedWithOffset,
                              expected, JetPsiUtil.isSelectorInQualified(expression));
        }
    }
}
