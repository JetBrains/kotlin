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

package org.jetbrains.jet.jvm.compiler;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.constants.AnnotationValue;
import org.jetbrains.jet.lang.resolve.constants.ArrayValue;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import org.jetbrains.jet.JetTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.java.DescriptorResolverUtils.getValueParameterDescriptorForAnnotationParameter;

public class AnnotationJavaDescriptorResolverTest extends AbstractJavaResolverDescriptorTest {

    private static final String PATH = "compiler/testData/javaDescriptorResolver/annotations/";
    private static final String DEFAULT_PACKAGE = "annotations";

    public void testCustomAnnotationWithKotlinEnum() throws IOException {
        File testFile = new File(PATH + "kotlinEnum.kt");

        LoadDescriptorUtil.compileKotlinToDirAndGetAnalyzeExhaust(testFile, tmpdir, myTestRootDisposable, ConfigurationKind.JDK_ONLY);

        StringBuilder builder = new StringBuilder(tmpdir.getAbsolutePath());
        builder.append(File.pathSeparator);
        File runtimePath = JetTestUtils.getPathsForTests().getRuntimePath();
        if (runtimePath.exists()) {
            builder.append(runtimePath.getAbsolutePath());
            builder.append(File.pathSeparator);
        }

        File annotationsPath = JetTestUtils.getPathsForTests().getJdkAnnotationsPath();
        if (annotationsPath.exists()) {
            builder.append(annotationsPath.getAbsolutePath());
        }

        compileJavaFile("customAnnotationWithKotlinEnum.java", builder.toString());

        String annotationTypeName = DEFAULT_PACKAGE + ".MyAnnotation";
        AnnotationDescriptor annotation = getAnnotationInClassByType("testClass", annotationTypeName);

        CompileTimeConstant<?> actualCompileTimeConstant = getCompileTimeConstant(annotation, annotationTypeName, "value");
        checkSimpleCompileTimeConstant(actualCompileTimeConstant, DEFAULT_PACKAGE + ".MyEnum", "MyEnum.ONE");
    }

    public void testCustomAnnotation() throws IOException {
        compileJavaFile("customAnnotation.java", null);
        String annotationTypeName = DEFAULT_PACKAGE + ".MyAnnotation";
        AnnotationDescriptor annotation = getAnnotationInClassByType("MyTest", annotationTypeName);

        CompileTimeConstant<?> actualCompileTimeConstant = getCompileTimeConstant(annotation, annotationTypeName, "value");
        checkSimpleCompileTimeConstant(actualCompileTimeConstant, DEFAULT_PACKAGE + ".MyEnum", "MyEnum.ONE");
    }

    public void testCustomAnnotationWithDefaultParameter() throws IOException {
        compileJavaFile("customAnnotationWithDefaultParameter.java", null);
        String annotationTypeName = DEFAULT_PACKAGE + ".MyAnnotation";
        AnnotationDescriptor annotation = getAnnotationInClassByType("MyTest", annotationTypeName);
        CompileTimeConstant<?> actualCompileTimeConstant = getCompileTimeConstant(annotation, annotationTypeName, "first");
        checkSimpleCompileTimeConstant(actualCompileTimeConstant, "jet.String", "\"f\"");

        actualCompileTimeConstant = getCompileTimeConstant(annotation, annotationTypeName, "second");
        checkSimpleCompileTimeConstant(actualCompileTimeConstant, "jet.String", "\"s\"");
    }

    public void testAnnotationWithAnnotationInParam() throws IOException {
        compileJavaFile("annotationWithAnnotationInParam.java", null);
        String annotationTypeName = DEFAULT_PACKAGE + ".MyAnnotationWithParam";
        AnnotationDescriptor annotation = getAnnotationInClassByType("A",
                                                                     annotationTypeName);
        CompileTimeConstant<?> actualCompileTimeConstant = getCompileTimeConstant(annotation, annotationTypeName, "value");
        assert actualCompileTimeConstant instanceof AnnotationValue;
        checkAnnotationCompileTimeConstant((AnnotationValue) actualCompileTimeConstant, "value",
                                           DEFAULT_PACKAGE + ".MyAnnotation", "jet.String", "\"test\"");

        String annotationTypeName2 = DEFAULT_PACKAGE + ".MyAnnotationWithParam2";
        AnnotationDescriptor annotation2 = getAnnotationInClassByType("B", annotationTypeName2);
        actualCompileTimeConstant = getCompileTimeConstant(annotation2, annotationTypeName2, "value");
        assert actualCompileTimeConstant instanceof AnnotationValue;
        checkAnnotationCompileTimeConstant((AnnotationValue) actualCompileTimeConstant, "value",
                                           DEFAULT_PACKAGE + ".MyAnnotation2", "jet.Array<jet.String>?", "[\"test\", \"test2\"]");

        String annotationTypeName3 = DEFAULT_PACKAGE + ".MyAnnotationWithParam3";
        AnnotationDescriptor annotation3 = getAnnotationInClassByType("C", annotationTypeName3);

        actualCompileTimeConstant = getCompileTimeConstant(annotation3, annotationTypeName3, "value");
        assert actualCompileTimeConstant instanceof AnnotationValue;
        checkAnnotationCompileTimeConstant((AnnotationValue) actualCompileTimeConstant, "first",
                                           DEFAULT_PACKAGE + ".MyAnnotation3", "jet.String", "\"f\"");
        checkAnnotationCompileTimeConstant((AnnotationValue) actualCompileTimeConstant, "second",
                                           DEFAULT_PACKAGE + ".MyAnnotation3", "jet.String", "\"s\"");
    }

    public void testRecursiveAnnotation() throws IOException {
        compileJavaFile("recursiveAnnotation.java", null);
        String annotationTypeName = DEFAULT_PACKAGE + ".B";
        AnnotationDescriptor annotation = getAnnotationInClassByType("A", annotationTypeName);
        CompileTimeConstant<?> actualCompileTimeConstant = getCompileTimeConstant(annotation, annotationTypeName, "value");
        assert actualCompileTimeConstant instanceof AnnotationValue;
        checkAnnotationCompileTimeConstant((AnnotationValue) actualCompileTimeConstant, "value", 
                                           DEFAULT_PACKAGE + ".A", "jet.String", "\"test\"");

        AnnotationDescriptor annotation2 = getAnnotationInClassByType("B", annotationTypeName);
        actualCompileTimeConstant = getCompileTimeConstant(annotation2, annotationTypeName, "value");
        assert actualCompileTimeConstant instanceof AnnotationValue;
        checkAnnotationCompileTimeConstant((AnnotationValue) actualCompileTimeConstant, "value", 
                                           DEFAULT_PACKAGE + ".A", "jet.String", "\"test\"");
    }

    public void testRecursiveAnnotation2() throws IOException {
        compileJavaFile("recursiveAnnotation2.java", null);
        String annotationTypeName = DEFAULT_PACKAGE + ".A";
        AnnotationDescriptor annotation = getAnnotationInClassByType("B", annotationTypeName);
        CompileTimeConstant<?> actualCompileTimeConstant = getCompileTimeConstant(annotation, annotationTypeName, "value");
        assert actualCompileTimeConstant instanceof AnnotationValue;
        checkAnnotationCompileTimeConstant((AnnotationValue) actualCompileTimeConstant, "value", 
                                           DEFAULT_PACKAGE + ".B", "jet.String", "\"test\"");
    }

    public void testAnnotationWithEnumInParam() throws IOException {
        compileJavaFile("annotationWithEnumInParam.java", null);
        String annotationTypeName = "java.lang.annotation.Retention";
        AnnotationDescriptor annotation = getAnnotationInClassByType("retentionAnnotation", annotationTypeName);
        CompileTimeConstant<?> actualCompileTimeConstant = getCompileTimeConstant(annotation, annotationTypeName, "value");
        checkSimpleCompileTimeConstant(actualCompileTimeConstant, "java.lang.annotation.RetentionPolicy", "RetentionPolicy.RUNTIME");
    }

    public void testAnnotationWithArrayOfEnumInParam() throws IOException {
        compileJavaFile("annotationWithArrayOfEnumInParam.java", null);
        String annotationTypeName = "java.lang.annotation.Target";
        AnnotationDescriptor annotation = getAnnotationInClassByType("targetAnnotation", annotationTypeName);
        assertEquals("Number of arguments is incorrect", 1, annotation.getAllValueArguments().size());
        String[] values = new String[] {"ElementType.FIELD", "ElementType.CONSTRUCTOR"};

        CompileTimeConstant<?> actualCompileTimeConstant = getCompileTimeConstant(annotation, annotationTypeName, "value");
        assert actualCompileTimeConstant instanceof ArrayValue;
        checkArrayCompileTimeConstant((ArrayValue) actualCompileTimeConstant, "jet.Array<java.lang.annotation.ElementType>?",
                                      "java.lang.annotation.ElementType", values);
    }

    public void testAnnotationWithArrayOfStringInParam() throws IOException {
        compileJavaFile("annotationWithArrayOfStringInParam.java", null);
        String annotationTypeName = DEFAULT_PACKAGE + ".MyAnnotation";
        AnnotationDescriptor annotation = getAnnotationInClassByType("A", annotationTypeName);
        assertEquals("Number of arguments is incorrect", 1, annotation.getAllValueArguments().size());
        String[] values = new String[] {"\"a\"", "\"b\"", "\"c\""};

        CompileTimeConstant<?> actualCompileTimeConstant = getCompileTimeConstant(annotation, annotationTypeName, "value");
        assert actualCompileTimeConstant instanceof ArrayValue;
        checkArrayCompileTimeConstant((ArrayValue) actualCompileTimeConstant, "jet.Array<jet.String>?", "jet.String", values);
    }

    public void testAnnotationWithEmptyArrayInParam() throws IOException {
        compileJavaFile("annotationWithEmptyArrayInParam.java", null);
        String annotationTypeName = DEFAULT_PACKAGE + ".MyAnnotation";
        AnnotationDescriptor annotation = getAnnotationInClassByType("A", annotationTypeName);

        assertEquals("Number of arguments is incorrect", 1, annotation.getAllValueArguments().size());
        String[] values = new String[] {};

        CompileTimeConstant<?> actualCompileTimeConstant = getCompileTimeConstant(annotation, annotationTypeName, "value");
        assert actualCompileTimeConstant instanceof ArrayValue;
        checkArrayCompileTimeConstant((ArrayValue) actualCompileTimeConstant, "jet.Array<jet.String>?", "jet.String", values);
    }

    private static void compareJetTypeWithClass(@NotNull JetType actualType, @NotNull String expectedType) {
        assertEquals(expectedType, DescriptorRenderer.TEXT.renderType(actualType));
    }

    @NotNull
    private CompileTimeConstant<?> getCompileTimeConstant(
            @NotNull AnnotationDescriptor annotationDescriptor,
            @NotNull String annotationType,
            @NotNull String parameterName
    ) {
        ValueParameterDescriptor valueParameterDescriptor = getValueParameterDescriptor(annotationType, parameterName);
        CompileTimeConstant<?> actualCompileTimeValue = annotationDescriptor.getValueArgument(valueParameterDescriptor);
        assertNotNull(actualCompileTimeValue);
        return actualCompileTimeValue;
    }

    @NotNull
    private ValueParameterDescriptor getValueParameterDescriptor(@NotNull String annotationTypeName, @NotNull String parameterName) {
        FqName fqName = new FqName(annotationTypeName);
        ClassDescriptor clazz = resolveClass(fqName);
        assertNotNull("Cannot resolve class with name " + annotationTypeName, clazz);
        ValueParameterDescriptor valueParameterDescriptor =
                getValueParameterDescriptorForAnnotationParameter(Name.identifier(parameterName), clazz);
        assertNotNull("Cannot resolve value parameter for " + parameterName, valueParameterDescriptor);
        return valueParameterDescriptor;
    }

    private ClassDescriptor resolveClass(FqName fqName) {
        PsiClass psiClass = testCoreEnvironment.getPsiClassFinder().findPsiClass(fqName);
        return testCoreEnvironment.getJavaDescriptorResolver().resolveClass(psiClass);
    }

    @NotNull
    private AnnotationDescriptor getAnnotationInClassByType(@NotNull String className, @NotNull String type) throws IOException {
        ClassDescriptor classDescriptor = resolveClass(new FqName(DEFAULT_PACKAGE + "." + className));
        assertNotNull("Cannot resolve class with name " + className, classDescriptor);
        List<AnnotationDescriptor> annotations = classDescriptor.getAnnotations();
        assertEquals(annotations.size(), 1);
        for (AnnotationDescriptor annotation : annotations) {
            if (type.endsWith(annotation.getType().toString())) {
                compareJetTypeWithClass(annotation.getType(), type);
                return annotation;
            }
        }
        fail("Cannot find annotation for class " + className + ", type " + type);
        return null;
    }

    private static void checkSimpleCompileTimeConstant(@NotNull CompileTimeConstant<?> actual, @NotNull String expectedType, @NotNull String expectedValue) {
        assertEquals(expectedValue, actual.toString());
        compareJetTypeWithClass(actual.getType(KotlinBuiltIns.getInstance()), expectedType);
    }

    private void checkAnnotationCompileTimeConstant(
            @NotNull AnnotationValue actual,
            @NotNull String parameterName,
            @NotNull String expectedType,
            @NotNull String expectedParameterType,
            @NotNull String expectedParameterValue
    ) {
        compareJetTypeWithClass(actual.getType(KotlinBuiltIns.getInstance()), expectedType);
        CompileTimeConstant<?> innerAnnotation = getCompileTimeConstant(actual.getValue(), expectedType, parameterName);
        checkSimpleCompileTimeConstant(innerAnnotation, expectedParameterType, expectedParameterValue);
    }

    private static void checkArrayCompileTimeConstant(
            @NotNull ArrayValue actual,
            @NotNull String expectedType,
            @NotNull String expectedArgumentType,
            @NotNull String[] expectedValues
    ) {
        JetType actualType = actual.getType(KotlinBuiltIns.getInstance());
        compareJetTypeWithClass(actualType, expectedType);

        List<CompileTimeConstant<?>> arrayValuesCompileTimeConst = actual.getValue();

        assertEquals("Number of arguments is incorrect", expectedValues.length, arrayValuesCompileTimeConst.size());
        int i = 0;
        for (CompileTimeConstant<?> constant : arrayValuesCompileTimeConst) {
            checkSimpleCompileTimeConstant(constant, expectedArgumentType, expectedValues[i]);
            i++;
        }
    }

    @NotNull
    @Override
    protected String getPath() {
        return PATH;
    }
}
