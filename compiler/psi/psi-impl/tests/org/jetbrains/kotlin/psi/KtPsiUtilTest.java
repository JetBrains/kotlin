/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.util.PsiTreeUtil;
import kotlin.jvm.functions.Function1;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.resolve.ImportPath;
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.IOException;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KtPsiUtilTest extends KotlinTestWithEnvironment {
    private final TestInfo testInfo;

    public KtPsiUtilTest(TestInfo info) {
        testInfo = info;
    }

    @Test
    public void testUnquotedIdentifier() {
        Assertions.assertEquals("", KtPsiUtil.unquoteIdentifier(""));
        Assertions.assertEquals("a2", KtPsiUtil.unquoteIdentifier("a2"));
        Assertions.assertEquals("", KtPsiUtil.unquoteIdentifier("``"));
        Assertions.assertEquals("a2", KtPsiUtil.unquoteIdentifier("`a2`"));
    }

    @Test
    public void testUnquotedIdentifierOrFieldReference() {
        Assertions.assertEquals("", KtPsiUtil.unquoteIdentifierOrFieldReference(""));
        Assertions.assertEquals("a2", KtPsiUtil.unquoteIdentifierOrFieldReference("a2"));
        Assertions.assertEquals("", KtPsiUtil.unquoteIdentifierOrFieldReference("``"));
        Assertions.assertEquals("a2", KtPsiUtil.unquoteIdentifierOrFieldReference("`a2`"));
        Assertions.assertEquals("$a2", KtPsiUtil.unquoteIdentifierOrFieldReference("$a2"));
        Assertions.assertEquals("$a2", KtPsiUtil.unquoteIdentifierOrFieldReference("$`a2`"));
    }

    @Test
    public void testConvertToImportPath() {
        Assertions.assertNull(getImportPathFromParsed("import "));
        Assertions.assertEquals(new ImportPath(new FqName("some"), false), getImportPathFromParsed("import some."));
        Assertions.assertNull(getImportPathFromParsed("import *"));
        Assertions.assertEquals(new ImportPath(new FqName("some.test"), true), getImportPathFromParsed("import some.test.* as SomeTest"));
        Assertions.assertEquals(new ImportPath(new FqName("some"), false), getImportPathFromParsed("import some?.Test"));

        Assertions.assertEquals(new ImportPath(new FqName("some"), false), getImportPathFromParsed("import some"));
        Assertions.assertEquals(new ImportPath(new FqName("some"), true), getImportPathFromParsed("import some.*"));
        Assertions.assertEquals(new ImportPath(new FqName("some.Test"), false), getImportPathFromParsed("import some.Test"));
        Assertions.assertEquals(new ImportPath(new FqName("some.test"), true), getImportPathFromParsed("import some.test.*"));
        Assertions.assertEquals(new ImportPath(new FqName("some.test"), false, Name.identifier("SomeTest")),
                                getImportPathFromParsed("import some.test as SomeTest"));

        Assertions.assertEquals(new ImportPath(new FqName("some.Test"), false),
                                getImportPathFromParsed("import some./* hello world */Test"));
        Assertions.assertEquals(new ImportPath(new FqName("some.Test"), false), getImportPathFromParsed("import some.    Test"));

        Assertions.assertNotSame(new ImportPath(new FqName("some.test"), false), getImportPathFromParsed("import some.Test"));
    }

    @Test
    public void testIsSingleQuoted() {
        KtPsiFactory factory = new KtPsiFactory(getProject());

        Assertions.assertTrue(KtPsiUtilKt.isSingleQuoted(factory.createStringTemplate("")));
        Assertions.assertTrue(KtPsiUtilKt.isSingleQuoted(factory.createStringTemplate("foo")));
        Assertions.assertTrue(KtPsiUtilKt.isSingleQuoted(factory.createMultiDollarStringTemplate("bar", 2, false)));
        Assertions.assertTrue(KtPsiUtilKt.isSingleQuoted(factory.createMultiDollarStringTemplate("baz", 5, false)));

        Assertions.assertFalse(KtPsiUtilKt.isSingleQuoted(factory.createMultiDollarStringTemplate("Foo\nBar", 2, false)));
        Assertions.assertFalse(KtPsiUtilKt.isSingleQuoted(factory.createMultiDollarStringTemplate("Foo", 2, true)));
    }

    @Test
    public void testPlainContent() {
        KtPsiFactory factory = new KtPsiFactory(getProject());
        String singleLineContent = "foo";
        String multiLineContent = "Bar\nBaz";

        Assertions.assertEquals("", KtPsiUtilKt.getPlainContent(factory.createStringTemplate("")));
        Assertions.assertEquals(singleLineContent, KtPsiUtilKt.getPlainContent(factory.createStringTemplate(singleLineContent)));
        Assertions.assertEquals(singleLineContent,
                                KtPsiUtilKt.getPlainContent(factory.createMultiDollarStringTemplate(singleLineContent, 2, false)));
        Assertions.assertEquals(singleLineContent,
                                KtPsiUtilKt.getPlainContent(factory.createMultiDollarStringTemplate(singleLineContent, 5, false)));

        Assertions.assertEquals(multiLineContent,
                                KtPsiUtilKt.getPlainContent(factory.createMultiDollarStringTemplate(multiLineContent, 2, false)));
        Assertions.assertEquals(singleLineContent,
                                KtPsiUtilKt.getPlainContent(factory.createMultiDollarStringTemplate(singleLineContent, 2, true)));
    }

    @Test
    public void testIsLocalClass() throws IOException {
        String text = FileUtil.loadFile(ForTestCompileRuntime.transformTestDataPath("compiler/psi/psi-impl/testData/psiUtil/isLocalClass.kt"), true);
        KtClass aClass = new KtPsiFactory(getProject()).createClass(text);

        Collection<KtClassOrObject> classOrObjects = PsiTreeUtil.collectElementsOfType(aClass, KtClassOrObject.class);

        for (KtClassOrObject classOrObject : classOrObjects) {
            String classOrObjectName = classOrObject.getName();
            if (classOrObjectName != null && classOrObjectName.contains("Local")) {
                Assertions.assertTrue(KtPsiUtil.isLocal(classOrObject), () -> "KtPsiUtil.isLocal should return true for " + classOrObjectName);
            }
            else {
                Assertions.assertFalse(KtPsiUtil.isLocal(classOrObject), () -> "KtPsiUtil.isLocal should return false for " + classOrObjectName);
            }
        }
    }

    @Test
    public void testIsSelectorInExpression() {
        checkIsSelectorInQualified();
    }

    @Test
    public void testIsSelectorInType() {
        checkIsSelectorInQualified();
    }

    @Test
    public void testIsCallee() {
        checkExpression(KtPsiUtilKt::isCallee);
    }

    private ImportPath getImportPathFromParsed(String text) {
        KtImportDirective importDirective =
                PsiTreeUtil.findChildOfType(new KtPsiFactory(getProject()).createFile(text), KtImportDirective.class);

        Assertions.assertNotNull(importDirective, "At least one import directive is expected");

        return importDirective.getImportPath();
    }

    private void checkIsSelectorInQualified() {
        checkExpression(KtPsiUtil::isSelectorInQualified);
    }

    private void checkExpression(Function1<KtSimpleNameExpression, Boolean> checkedFunction) {
        String trueResultString = "/*true*/";
        String falseResultString = "/*false*/";

        String testName = testInfo.getTestMethod().get().getName();
        String name = KtUsefulTestCase.getTestName(testName, true) + ".kt";
        String fileText = KtTestUtil.doLoadFile(ForTestCompileRuntime.transformTestDataPath("compiler/psi/psi-impl/testData/psiUtil/" + name));
        KtFile file = KtTestUtil.createFile(name, fileText, getProject());

        String text = file.getText();

        // /*true*/|/*false*/
        Pattern pattern = Pattern.compile(String.format("%s|%s", Pattern.quote(trueResultString), Pattern.quote(falseResultString)));
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            boolean expected = trueResultString.equals(matcher.group());
            int offset = matcher.end();

            KtSimpleNameExpression expression = PsiTreeUtil.findElementOfClassAtOffset(file, offset, KtSimpleNameExpression.class, true);

            String modifiedWithOffset = new StringBuilder(text).insert(offset, "<======caret======>").toString();

            Assertions.assertNotNull(expression, "Can't find expression in text:\n" + modifiedWithOffset);
            Assertions.assertSame(expected, checkedFunction.invoke(expression),
                                  expected + " result was expected at\n" + modifiedWithOffset);
        }
    }
}
