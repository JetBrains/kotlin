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

import jet.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.descriptors.serialization.*;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KotlinPackageAnnotationTest extends CodegenTestCase {
    public static final FqName NAMESPACE_NAME = new FqName("test");

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    public void testPackageKotlinInfo() throws Exception {
        loadText("package " + NAMESPACE_NAME + "\n" +
                 "\n" +
                 "fun foo() = 42\n" +
                 "val bar = 239\n" +
                 "\n" +
                 "class A\n" +
                 "class B\n" +
                 "object C\n");
        Class aClass = generateClass(PackageClassUtils.getPackageClassFqName(NAMESPACE_NAME).asString());

        Class<? extends Annotation> annotationClass = getCorrespondingAnnotationClass(KotlinPackage.class);
        assertTrue(aClass.isAnnotationPresent(annotationClass));
        assertTrue(aClass.isAnnotationPresent(annotationClass));

        Annotation kotlinPackage = aClass.getAnnotation(annotationClass);

        PackageData data = JavaProtoBufUtil.readPackageDataFrom((String[]) ClassLoaderIsolationUtil.getAnnotationAttribute(kotlinPackage,
                                                                                                                           "data"));

        Set<String> callableNames = collectCallableNames(data.getPackageProto().getMemberList(), data.getNameResolver());
        assertSameElements(Arrays.asList("foo", "bar", "C"), callableNames);
    }

    @NotNull
    public static Set<String> collectCallableNames(@NotNull List<ProtoBuf.Callable> members, @NotNull NameResolver nameResolver) {
        Set<String> callableNames = new HashSet<String>();
        for (ProtoBuf.Callable callable : members) {
            callableNames.add(nameResolver.getName(callable.getName()).asString());
        }
        return callableNames;
    }
}
