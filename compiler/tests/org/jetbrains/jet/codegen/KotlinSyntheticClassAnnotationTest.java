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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.OutputFile;
import org.jetbrains.jet.OutputFileCollection;
import org.jetbrains.jet.lang.resolve.java.AbiVersionUtil;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.lang.annotation.Annotation;

import static org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames.ABI_VERSION_FIELD_NAME;
import static org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames.KotlinSyntheticClass;

public class KotlinSyntheticClassAnnotationTest extends CodegenTestCase {
    public static final FqName PACKAGE_NAME = new FqName("test");

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    public void testAnnotationIsWrittenOnPackagePart() throws Exception {
        loadText("package " + PACKAGE_NAME + "\n\nfun foo() = 42\n");
        String facadeFileName = JvmClassName.byFqNameWithoutInnerClasses(PackageClassUtils.getPackageClassFqName(PACKAGE_NAME)).getInternalName() + ".class";

        OutputFileCollection outputFiles = generateClassesInFile();
        for (OutputFile outputFile : outputFiles.asList()) {
            // The file which is not a facade is a package part
            String filePath = outputFile.getRelativePath();
            if (filePath.equals(facadeFileName)) continue;

            String fqName = filePath.substring(0, filePath.length() - ".class".length()).replace('/', '.');
            Class<?> aClass = generateClass(fqName);

            assertAnnotatedWithKind(aClass, "PACKAGE_PART");
            return;
        }

        fail("No package part was found: " + outputFiles.asList());
    }

    public void testAnnotationIsWrittenOnTraitImpl() throws Exception {
        loadText("package " + PACKAGE_NAME + "\n\ntrait A { fun foo() = 42 }\n");

        Class<?> aClass = generateClass(PACKAGE_NAME + ".A" + JvmAbi.TRAIT_IMPL_SUFFIX);
        assertNotNull("TImpl is not generated", aClass);
        assertAnnotatedWithKind(aClass, "TRAIT_IMPL");
    }

    private void assertAnnotatedWithKind(@NotNull Class<?> aClass, @NotNull String expectedKind) {
        Class<? extends Annotation> annotationClass = loadAnnotationClassQuietly(KotlinSyntheticClass.FQ_NAME.asString());
        assertTrue("No KotlinSyntheticClass annotation found", aClass.isAnnotationPresent(annotationClass));

        Annotation annotation = aClass.getAnnotation(annotationClass);

        Integer version = (Integer) CodegenTestUtil.getAnnotationAttribute(annotation, ABI_VERSION_FIELD_NAME);
        assertNotNull(version);
        assertTrue("KotlinSyntheticClass annotation is written with an unsupported format", AbiVersionUtil.isAbiVersionCompatible(version));

        Object actualKind = CodegenTestUtil.getAnnotationAttribute(annotation, KotlinSyntheticClass.KIND_FIELD_NAME.asString());
        assertNotNull(actualKind);
        assertEquals("KotlinSyntheticClass annotation has the wrong kind", expectedKind, actualKind.toString());
    }
}
