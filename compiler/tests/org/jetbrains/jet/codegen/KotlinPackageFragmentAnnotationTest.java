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

import jet.KotlinPackageFragment;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.lang.resolve.java.AbiVersionUtil;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.lang.annotation.Annotation;

public class KotlinPackageFragmentAnnotationTest extends CodegenTestCase {
    public static final FqName NAMESPACE_NAME = new FqName("test");

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    public void testKotlinPackageFragmentIsWritten() throws Exception {
        loadText("package " + NAMESPACE_NAME + "\n\nfun foo() = 42\n");
        String facadeFileName = JvmClassName.byFqNameWithoutInnerClasses(PackageClassUtils.getPackageClassFqName(NAMESPACE_NAME)).getInternalName() + ".class";

        ClassFileFactory factory = generateClassesInFile();
        for (String fileName : factory.files()) {
            if (!fileName.equals(facadeFileName)) {
                // The file which is not a facade is a package fragment
                String fqName = fileName.substring(0, fileName.length() - ".class".length()).replace('/', '.');
                Class aClass = generateClass(fqName);

                Class<? extends Annotation> annotationClass = getCorrespondingAnnotationClass(KotlinPackageFragment.class);

                assertTrue("No KotlinPackageFragment annotation on a package fragment",
                           aClass.isAnnotationPresent(annotationClass));

                Annotation kotlinPackageFragment = aClass.getAnnotation(annotationClass);

                assertTrue("KotlinPackageFragment annotation is written with an unsupported format",
                           AbiVersionUtil.isAbiVersionCompatible(
                                   (Integer) ClassLoaderIsolationUtil.getAnnotationAttribute(kotlinPackageFragment, "abiVersion")));

                return;
            }
        }

        fail("No package fragment was found: " + factory.files());
    }
}
