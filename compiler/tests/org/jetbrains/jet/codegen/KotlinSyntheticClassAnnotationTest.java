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

package org.jetbrains.jet.codegen;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.OutputFile;
import org.jetbrains.jet.lang.resolve.java.AbiVersionUtil;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames.ABI_VERSION_FIELD_NAME;
import static org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames.KotlinSyntheticClass;
import static org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames.KotlinSyntheticClass.Kind.*;

public class KotlinSyntheticClassAnnotationTest extends CodegenTestCase {
    public static final FqName PACKAGE_NAME = new FqName("test");

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL);
    }

    public void testPackagePart() {
        doTest("fun foo() = 42",
               "$",
               PACKAGE_PART, false);
    }

    public void testTraitImpl() {
        doTest("trait A { fun foo() = 42 }",
               JvmAbi.TRAIT_IMPL_SUFFIX,
               TRAIT_IMPL, true);
    }

    public void testSamWrapper() {
        doTest("val f = {}\nval foo = Thread(f)",
               "$sam",
               SAM_WRAPPER, false);
    }

    public void testSamLambda() {
        doTest("val foo = Thread { }",
               "$1",
               SAM_LAMBDA, true);
    }

    public void testCallableReferenceWrapper() {
        doTest("val f = String::get",
               "$1",
               CALLABLE_REFERENCE_WRAPPER, true);
    }

    public void testLocalFunction() {
        doTest("fun foo() { fun bar() {} }",
               "$1",
               LOCAL_FUNCTION, true);
    }

    public void testAnonymousFunction() {
        doTest("val f = {}",
               "$1",
               ANONYMOUS_FUNCTION, true);
    }

    public void testLocalClass() {
        doTest("fun foo() { class Local }",
               "Local",
               LOCAL_CLASS, true);
    }

    public void testLocalTraitImpl() {
        doTest("fun foo() { trait Local { fun bar() = 42 } }",
               "Local$$TImpl",
               LOCAL_CLASS, true);
    }

    public void testLocalTraitInterface() {
        doTest("fun foo() { trait Local { fun bar() = 42 } }",
               "Local",
               LOCAL_CLASS, true);
    }

    public void testInnerClassOfLocalClass() {
        doTest("fun foo() { class Local { inner class Inner } }",
               "Inner",
               LOCAL_CLASS, true);
    }

    public void testAnonymousObject() {
        doTest("val o = object {}",
               "$1",
               ANONYMOUS_OBJECT, true);
    }

    private void doTest(
            @NotNull String code,
            @NotNull final String classNamePart,
            @NotNull KotlinSyntheticClass.Kind expectedKind,
            final boolean endWithNotContains
    ) {
        loadText("package " + PACKAGE_NAME + "\n\n" + code);
        List<OutputFile> output = generateClassesInFile().asList();
        Collection<OutputFile> files = Collections2.filter(output, new Predicate<OutputFile>() {
            @Override
            public boolean apply(OutputFile file) {
                String path = file.getRelativePath();
                return endWithNotContains ? path.endsWith(classNamePart + ".class") : path.contains(classNamePart);
            }
        });
        assertFalse("No files with \"" + classNamePart + "\" in the name are found: " + output, files.isEmpty());
        assertTrue("Exactly one file with \"" + classNamePart + "\" in the name should be found: " + files, files.size() == 1);

        String path = files.iterator().next().getRelativePath();
        String fqName = path.substring(0, path.length() - ".class".length()).replace('/', '.');
        Class<?> aClass = generateClass(fqName);
        assertAnnotatedWithKind(aClass, expectedKind);
    }

    private void assertAnnotatedWithKind(@NotNull Class<?> aClass, @NotNull KotlinSyntheticClass.Kind expectedKind) {
        Class<? extends Annotation> annotationClass = loadAnnotationClassQuietly(
                KotlinSyntheticClass.CLASS_NAME.getFqNameForClassNameWithoutDollars().asString());
        assertTrue("No KotlinSyntheticClass annotation found", aClass.isAnnotationPresent(annotationClass));

        Annotation annotation = aClass.getAnnotation(annotationClass);

        Integer version = (Integer) CodegenTestUtil.getAnnotationAttribute(annotation, ABI_VERSION_FIELD_NAME);
        assertNotNull(version);
        assertTrue("KotlinSyntheticClass annotation is written with an unsupported format", AbiVersionUtil.isAbiVersionCompatible(version));

        Object actualKind = CodegenTestUtil.getAnnotationAttribute(annotation, KotlinSyntheticClass.KIND_FIELD_NAME.asString());
        assertNotNull(actualKind);
        assertEquals("KotlinSyntheticClass annotation has the wrong kind", expectedKind.toString(), actualKind.toString());
    }
}
