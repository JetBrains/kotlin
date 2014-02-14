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

import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.OutputFile;
import org.jetbrains.jet.OutputFileCollection;
import org.jetbrains.jet.lang.resolve.java.AbiVersionUtil;
import org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.lang.annotation.Annotation;

public class KotlinPackagePartAnnotationTest extends CodegenTestCase {
    public static final FqName PACKAGE_NAME = new FqName("test");

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    public void testKotlinPackagePartAnnotationIsWritten() throws Exception {
        loadText("package " + PACKAGE_NAME + "\n\nfun foo() = 42\n");
        String facadeFileName = JvmClassName.byFqNameWithoutInnerClasses(PackageClassUtils.getPackageClassFqName(PACKAGE_NAME)).getInternalName() + ".class";

        OutputFileCollection outputFiles = generateClassesInFile();
        for (OutputFile outputFile : outputFiles.asList()) {
            String filePath = outputFile.getRelativePath();

            if (!filePath.equals(facadeFileName)) {
                // The file which is not a facade is a package part
                String fqName = filePath.substring(0, filePath.length() - ".class".length()).replace('/', '.');
                Class<?> aClass = generateClass(fqName);

                Class<? extends Annotation> annotationClass = loadAnnotationClassQuietly(JvmAnnotationNames.KOTLIN_PACKAGE_PART.asString());

                assertTrue("No KotlinPackagePart annotation on a package part",
                           aClass.isAnnotationPresent(annotationClass));

                Annotation kotlinPackagePart = aClass.getAnnotation(annotationClass);

                Integer version = (Integer) CodegenTestUtil.getAnnotationAttribute(kotlinPackagePart, "abiVersion");
                assertNotNull(version);
                assertTrue("KotlinPackagePart annotation is written with an unsupported format",
                           AbiVersionUtil.isAbiVersionCompatible(version));

                return;
            }
        }

        fail("No package part was found: " + outputFiles.asList());
    }
}
