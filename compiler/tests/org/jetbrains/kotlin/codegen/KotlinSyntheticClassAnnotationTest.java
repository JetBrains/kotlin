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

import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.backend.common.output.OutputFile;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.load.kotlin.JvmMetadataVersion;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.test.ConfigurationKind;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.kotlin.load.java.JvmAnnotationNames.METADATA_FQ_NAME;
import static org.jetbrains.kotlin.load.java.JvmAnnotationNames.METADATA_VERSION_FIELD_NAME;

public class KotlinSyntheticClassAnnotationTest extends CodegenTestCase {
    public static final FqName PACKAGE_NAME = new FqName("test");

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL);
    }

    public void testTraitImpl() {
        doTestKotlinSyntheticClass(
                "interface A { fun foo() = 42 }",
                JvmAbi.DEFAULT_IMPLS_SUFFIX
        );
    }

    public void testSamWrapper() {
        doTestKotlinSyntheticClass(
                "val f = {}\nval foo = Thread(f)",
                "$sam"
        );
    }

    public void testSamLambda() {
        doTestKotlinSyntheticClass(
                "val foo = Thread { }",
                "$1"
        );
    }

    public void testCallableReferenceWrapper() {
        doTestKotlinSyntheticClass(
                "val f = String::get",
                "$1"
        );
    }

    public void testLocalFunction() {
        doTestKotlinSyntheticClass(
                "fun foo() { fun bar() {} }",
                "$1"
        );
    }

    public void testAnonymousFunction() {
        doTestKotlinSyntheticClass(
                "val f = {}",
                "$1"
        );
    }

    public void testLocalClass() {
        doTestKotlinClass(
                "fun foo() { class Local }",
                "Local"
        );
    }

    public void testInnerClassOfLocalClass() {
        doTestKotlinClass(
                "fun foo() { class Local { inner class Inner } }",
                "Inner"
        );
    }

    public void testAnonymousObject() {
        doTestKotlinClass(
                "val o = object {}",
                "$1"
        );
    }

    public void testWhenMappings() {
        doTestKotlinSyntheticClass(
                "enum class E { A }\n" +
                "val x = when (E.A) { E.A -> 1; else -> 0; }",
                "WhenMappings"
        );
    }

    private void doTestKotlinSyntheticClass(@NotNull String code, @NotNull String classFilePart) {
        doTest(code, classFilePart);
    }

    private void doTestKotlinClass(@NotNull String code, @NotNull String classFilePart) {
        doTest(code, classFilePart);
    }

    private void doTest(@NotNull String code, @NotNull String classFilePart) {
        loadText("package " + PACKAGE_NAME + "\n\n" + code);
        List<OutputFile> output = generateClassesInFile().asList();
        Collection<OutputFile> files = CollectionsKt.filter(output, file -> file.getRelativePath().contains(classFilePart));
        assertFalse("No files with \"" + classFilePart + "\" in the name are found: " + output, files.isEmpty());
        assertTrue("Exactly one file with \"" + classFilePart + "\" in the name should be found: " + files, files.size() == 1);

        String path = files.iterator().next().getRelativePath();
        String fqName = path.substring(0, path.length() - ".class".length()).replace('/', '.');
        Class<?> aClass = generateClass(fqName);
        assertAnnotatedWithMetadata(aClass);
    }

    private void assertAnnotatedWithMetadata(@NotNull Class<?> aClass) {
        String annotationFqName = METADATA_FQ_NAME.asString();
        Class<? extends Annotation> annotationClass = loadAnnotationClassQuietly(annotationFqName);
        assertTrue("No annotation " + annotationFqName + " found in " + aClass, aClass.isAnnotationPresent(annotationClass));

        Annotation annotation = aClass.getAnnotation(annotationClass);

        int[] version = (int[]) CodegenTestUtil.getAnnotationAttribute(annotation, METADATA_VERSION_FIELD_NAME);
        assertNotNull(version);
        assertTrue("Annotation " + annotationFqName + " is written with an unsupported format",
                   new JvmMetadataVersion(version).isCompatible());
    }
}
