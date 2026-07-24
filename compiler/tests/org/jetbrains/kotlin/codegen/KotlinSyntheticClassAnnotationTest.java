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
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.JVMConfigurationKeys;
import org.jetbrains.kotlin.config.JvmClosureGenerationScheme;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.FirParser;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.kotlin.load.java.JvmAnnotationNames.METADATA_FQ_NAME;
import static org.jetbrains.kotlin.load.java.JvmAnnotationNames.METADATA_VERSION_FIELD_NAME;
import static org.junit.jupiter.api.Assertions.*;

public class KotlinSyntheticClassAnnotationTest extends CodegenTestCase {
    @Override
    public @NotNull FirParser getFirParser() {
        return FirParser.LightTree;
    }

    private static final FqName PACKAGE_NAME = new FqName("test");

    @BeforeEach
    protected void setUp() throws Exception {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL);
    }

    @Override
    protected void updateConfiguration(@NotNull CompilerConfiguration configuration) {
        configuration.put(JVMConfigurationKeys.LAMBDAS, JvmClosureGenerationScheme.CLASS);
        configuration.put(JVMConfigurationKeys.SAM_CONVERSIONS, JvmClosureGenerationScheme.CLASS);
        super.updateConfiguration(configuration);
    }

    @Test
    public void testTraitImpl() {
        doTestKotlinSyntheticClass(
                "interface A { fun foo() = 42 }",
                JvmAbi.DEFAULT_IMPLS_SUFFIX
        );
    }

    @Test
    public void testSamWrapper() {
        doTestKotlinSyntheticClass(
                "val f = {}\nval foo = Thread(f)",
                "$sam"
        );
    }

    @Test
    public void testSamLambda() {
        doTestKotlinSyntheticClass(
                "val foo = Thread { }",
                "$1"
        );
    }

    @Test
    public void testCallableReferenceWrapper() {
        doTestKotlinSyntheticClass(
                "val f = String::get",
                "$1"
        );
    }

    @Test
    public void testAnonymousFunction() {
        doTestKotlinSyntheticClass(
                "val f = {}",
                "$1"
        );
    }

    @Test
    public void testLocalClass() {
        doTestKotlinClass(
                "fun foo() { class Local }",
                "Local"
        );
    }

    @Test
    public void testInnerClassOfLocalClass() {
        doTestKotlinClass(
                "fun foo() { class Local { inner class Inner } }",
                "Inner"
        );
    }

    @Test
    public void testAnonymousObject() {
        doTestKotlinClass(
                "val o = object {}",
                "$1"
        );
    }

    @Test
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
        assertFalse(files.isEmpty(), "No files with \"" + classFilePart + "\" in the name are found: " + output);
        assertEquals(1, files.size(), "Exactly one file with \"" + classFilePart + "\" in the name should be found: " + files);

        String path = files.iterator().next().getRelativePath();
        String fqName = path.substring(0, path.length() - ".class".length()).replace('/', '.');
        Class<?> aClass = generateClass(fqName);
        assertAnnotatedWithMetadata(aClass);
    }

    private void assertAnnotatedWithMetadata(@NotNull Class<?> aClass) {
        String annotationFqName = METADATA_FQ_NAME.asString();
        Class<? extends Annotation> annotationClass = loadAnnotationClassQuietly(annotationFqName);
        assertTrue(aClass.isAnnotationPresent(annotationClass), "No annotation " + annotationFqName + " found in " + aClass);

        Annotation annotation = aClass.getAnnotation(annotationClass);

        int[] version = (int[]) CodegenTestUtil.getAnnotationAttribute(annotation, METADATA_VERSION_FIELD_NAME);
        assertNotNull(version);
        assertTrue(new MetadataVersion(version).isCompatibleWithCurrentCompilerVersion(),
                              "Annotation " + annotationFqName + " is written with an unsupported format");
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private Class<? extends Annotation> loadAnnotationClassQuietly(@NotNull String fqName) {
        try {
            return (Class<? extends Annotation>) initializedClassLoader.loadClass(fqName);
        }
        catch (ClassNotFoundException e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
    }
}
