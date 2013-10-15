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
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class SyntheticMethodForAnnotatedPropertyGenTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    @NotNull
    @Override
    protected String getPrefix() {
        return "properties/syntheticMethod";
    }

    private static final String TEST_SYNTHETIC_METHOD_NAME =
            JvmAbi.getSyntheticMethodSignatureForAnnotatedProperty(Name.identifier("property"), null).getName();

    public void testInClass() {
        loadFile();
        assertClassHasAnnotatedSyntheticMethod(generateClass("A"));
    }

    public void testTopLevel() {
        loadFile();
        String packageClassName = PackageClassUtils.getPackageClassName(FqName.ROOT);
        for (String fileName : generateClassesInFile().files()) {
            if (fileName.startsWith(packageClassName) && !fileName.equals(packageClassName + ".class")) {
                // This should be package$src class
                Class<?> a = generateClass(fileName.substring(0, fileName.length() - ".class".length()));
                assertClassHasAnnotatedSyntheticMethod(a);
            }
        }
    }

    private static void assertClassHasAnnotatedSyntheticMethod(@NotNull Class<?> a) {
        for (Method method : a.getDeclaredMethods()) {
            if (TEST_SYNTHETIC_METHOD_NAME.equals(method.getName())) {
                assertTrue(method.isSynthetic());
                int modifiers = method.getModifiers();
                assertTrue(Modifier.isFinal(modifiers));
                assertTrue(Modifier.isStatic(modifiers));
                assertTrue(Modifier.isPrivate(modifiers));

                Annotation[] annotations = method.getDeclaredAnnotations();
                assertSize(1, annotations);
                assertEquals("@SomeAnnotation(value=OK)", annotations[0].toString());
                return;
            }
        }
        fail("Synthetic method for annotated property not found: " + TEST_SYNTHETIC_METHOD_NAME);
    }
}
