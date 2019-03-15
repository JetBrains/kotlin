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

package org.jetbrains.kotlin.resolve.annotation;

import java.io.IOException;

public class AnnotationDescriptorResolveTest extends AbstractAnnotationDescriptorResolveTest {
    public void testIntAnnotation() throws IOException {
        String content = getContent("AnnInt(1)");
        String expectedAnnotation = "@AnnInt(a = 1)";
        doTest(content, expectedAnnotation);
    }

    public void testStringAnnotation() throws IOException {
        String content = getContent("AnnString(\"test\")");
        String expectedAnnotation = "@AnnString(a = \"test\")";
        doTest(content, expectedAnnotation);
    }

    public void testEnumAnnotation() throws IOException {
        String content = getContent("AnnEnum(MyEnum.A)");
        String expectedAnnotation = "@AnnEnum(a = MyEnum.A)";
        doTest(content, expectedAnnotation);
    }

    public void testQualifiedEnumAnnotation() throws IOException {
        String content = getContent("AnnEnum(MyEnum.A)");
        String expectedAnnotation = "@AnnEnum(a = MyEnum.A)";
        doTest(content, expectedAnnotation);
    }

    public void testUnqualifiedEnumAnnotation() throws IOException {
        String content = getContent("AnnEnum(A)");
        String expectedAnnotation = "@AnnEnum(a = MyEnum.A)";
        doTest(content, expectedAnnotation);
    }

    public void testIntArrayAnnotation() throws IOException {
        String content = getContent("AnnIntArray(intArrayOf(1, 2))");
        String expectedAnnotation = "@AnnIntArray(a = {1, 2})";
        doTest(content, expectedAnnotation);
    }

    public void testIntArrayVarargAnnotation() throws IOException {
        String content = getContent("AnnIntVararg(1, 2)");
        String expectedAnnotation = "@AnnIntVararg(a = {1, 2})";
        doTest(content, expectedAnnotation);
    }

    public void testStringArrayVarargAnnotation() throws IOException {
        String content = getContent("AnnStringVararg(\"a\", \"b\")");
        String expectedAnnotation = "@AnnStringVararg(a = {\"a\", \"b\"})";
        doTest(content, expectedAnnotation);
    }

    public void testStringArrayAnnotation() throws IOException {
        String content = getContent("AnnStringArray(arrayOf(\"a\"))");
        String expectedAnnotation = "@AnnStringArray(a = {\"a\"})";
        doTest(content, expectedAnnotation);
    }

    public void testEnumArrayAnnotation() throws IOException {
        String content = getContent("AnnArrayOfEnum(arrayOf(MyEnum.A))");
        String expectedAnnotation = "@AnnArrayOfEnum(a = {MyEnum.A})";
        doTest(content, expectedAnnotation);
    }

    public void testAnnotationAnnotation() throws Exception {
        String content = getContent("AnnAnn(AnnInt(1))");
        String expectedAnnotation = "@AnnAnn(a = AnnInt(a = 1))";
        doTest(content, expectedAnnotation);
    }

    public void testJavaClassAnnotation() throws Exception {
        String content = getContent("AnnClass(MyClass::class)");
        String expectedAnnotation = "@AnnClass(a = test.MyClass::class)";
        doTest(content, expectedAnnotation);
    }
}
