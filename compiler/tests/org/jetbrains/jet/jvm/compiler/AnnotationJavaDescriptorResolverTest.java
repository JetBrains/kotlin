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
        ClassDescriptor clazz = javaDescriptorResolver.resolveClass(new FqName(annotationTypeName));
        assertNotNull("Cannot resolve class with name " + annotationTypeName, clazz);
        ValueParameterDescriptor valueParameterDescriptor =
                getValueParameterDescriptorForAnnotationParameter(Name.identifier(parameterName), clazz);
        assertNotNull("Cannot resolve value parameter for " + parameterName, valueParameterDescriptor);
        return valueParameterDescriptor;
    }

    @NotNull
    private AnnotationDescriptor getAnnotationInClassByType(@NotNull String className, @NotNull String type) throws IOException {
        ClassDescriptor classDescriptor = javaDescriptorResolver.resolveClass(new FqName(DEFAULT_PACKAGE + "." + className));
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
