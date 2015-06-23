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

package org.jetbrains.kotlin.codegen;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.backend.common.output.OutputFile;
import org.jetbrains.kotlin.load.java.AbiVersionUtil;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.KotlinClass;
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.KotlinSyntheticClass;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.resolve.jvm.JvmClassName;
import org.jetbrains.kotlin.test.ConfigurationKind;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.kotlin.load.java.JvmAnnotationNames.ABI_VERSION_FIELD_NAME;
import static org.jetbrains.kotlin.load.java.JvmAnnotationNames.KIND_FIELD_NAME;
import static org.jetbrains.kotlin.load.java.JvmAnnotationNames.KotlinClass.Kind.ANONYMOUS_OBJECT;
import static org.jetbrains.kotlin.load.java.JvmAnnotationNames.KotlinClass.Kind.LOCAL_CLASS;
import static org.jetbrains.kotlin.load.java.JvmAnnotationNames.KotlinSyntheticClass.Kind.*;

public class KotlinSyntheticClassAnnotationTest extends CodegenTestCase {
    public static final FqName PACKAGE_NAME = new FqName("test");

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL);
    }

    public void testPackagePart() {
        doTestKotlinSyntheticClass(
                "fun foo() = 42",
                KotlinPackage.capitalize(DEFAULT_TEST_FILE_NAME),
                PACKAGE_PART
        );
    }

    public void testTraitImpl() {
        doTestKotlinSyntheticClass(
                "trait A { fun foo() = 42 }",
                JvmAbi.TRAIT_IMPL_SUFFIX,
                TRAIT_IMPL
        );
    }

    public void testSamWrapper() {
        doTestKotlinSyntheticClass(
                "val f = {}\nval foo = Thread(f)",
                "$sam",
                SAM_WRAPPER
        );
    }

    public void testSamLambda() {
        doTestKotlinSyntheticClass(
                "val foo = Thread { }",
                "$1",
                SAM_LAMBDA
        );
    }

    public void testCallableReferenceWrapper() {
        doTestKotlinSyntheticClass(
                "val f = String::get",
                "$1",
                CALLABLE_REFERENCE_WRAPPER
        );
    }

    public void testLocalFunction() {
        doTestKotlinSyntheticClass(
                "fun foo() { fun bar() {} }",
                "$1",
                LOCAL_FUNCTION
        );
    }

    public void testAnonymousFunction() {
        doTestKotlinSyntheticClass(
                "val f = {}",
                "$1",
                ANONYMOUS_FUNCTION
        );
    }

    public void testLocalClass() {
        doTestKotlinClass(
                "fun foo() { class Local }",
                "Local",
                LOCAL_CLASS
        );
    }

    public void testLocalTraitImpl() {
        doTestKotlinSyntheticClass(
                "fun foo() { trait Local { fun bar() = 42 } }",
                "Local$$TImpl.class",
                LOCAL_TRAIT_IMPL
        );
    }

    public void testLocalTraitInterface() {
        doTestKotlinClass(
                "fun foo() { trait Local { fun bar() = 42 } }",
                "Local.class",
                LOCAL_CLASS
        );
    }

    public void testInnerClassOfLocalClass() {
        doTestKotlinClass(
                "fun foo() { class Local { inner class Inner } }",
                "Inner",
                LOCAL_CLASS
        );
    }

    public void testAnonymousObject() {
        doTestKotlinClass(
                "val o = object {}",
                "$1",
                ANONYMOUS_OBJECT
        );
    }

    public void testWhenMappings() {
        doTestKotlinSyntheticClass(
                "enum class E { A }\n" +
                "val x = when (E.A) { E.A -> 1; else -> 0; }",
                "WhenMappings",
                WHEN_ON_ENUM_MAPPINGS
        );
    }

    private void doTestKotlinSyntheticClass(
            @NotNull String code,
            @NotNull String classFilePart,
            @NotNull KotlinSyntheticClass.Kind expectedKind
    ) {
        doTest(code, classFilePart, KotlinSyntheticClass.CLASS_NAME, expectedKind.toString());
    }

    private void doTestKotlinClass(
            @NotNull String code,
            @NotNull String classFilePart,
            @NotNull KotlinClass.Kind expectedKind
    ) {
        doTest(code, classFilePart, KotlinClass.CLASS_NAME, expectedKind.toString());
    }

    private void doTest(
            @NotNull String code,
            @NotNull final String classFilePart,
            @NotNull JvmClassName annotationName,
            @NotNull String expectedKind
    ) {
        loadText("package " + PACKAGE_NAME + "\n\n" + code);
        List<OutputFile> output = generateClassesInFile().asList();
        Collection<OutputFile> files = Collections2.filter(output, new Predicate<OutputFile>() {
            @Override
            public boolean apply(OutputFile file) {
                return file.getRelativePath().contains(classFilePart);
            }
        });
        assertFalse("No files with \"" + classFilePart + "\" in the name are found: " + output, files.isEmpty());
        assertTrue("Exactly one file with \"" + classFilePart + "\" in the name should be found: " + files, files.size() == 1);

        String path = files.iterator().next().getRelativePath();
        String fqName = path.substring(0, path.length() - ".class".length()).replace('/', '.');
        Class<?> aClass = generateClass(fqName);
        assertAnnotatedWithKind(aClass, annotationName.getFqNameForClassNameWithoutDollars().asString(), expectedKind);
    }

    private void assertAnnotatedWithKind(
            @NotNull Class<?> aClass,
            @NotNull String annotationFqName,
            @NotNull String expectedKind
    ) {
        Class<? extends Annotation> annotationClass = loadAnnotationClassQuietly(annotationFqName);
        assertTrue("No annotation " + annotationFqName + " found in " + aClass, aClass.isAnnotationPresent(annotationClass));

        Annotation annotation = aClass.getAnnotation(annotationClass);

        Integer version = (Integer) CodegenTestUtil.getAnnotationAttribute(annotation, ABI_VERSION_FIELD_NAME);
        assertNotNull(version);
        assertTrue("Annotation " + annotationFqName + " is written with an unsupported format",
                   AbiVersionUtil.isAbiVersionCompatible(version));

        Object actualKind = CodegenTestUtil.getAnnotationAttribute(annotation, KIND_FIELD_NAME);
        assertNotNull(actualKind);
        assertEquals("Annotation " + annotationFqName + " has the wrong kind", expectedKind, actualKind.toString());
    }
}
