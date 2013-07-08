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

import jet.KotlinInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.descriptors.serialization.JavaProtoBufUtil;
import org.jetbrains.jet.descriptors.serialization.PackageData;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KotlinInfoForPackageTest extends CodegenTestCase {
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

        assertTrue(aClass.isAnnotationPresent(KotlinInfo.class));
        KotlinInfo kotlinInfo = (KotlinInfo) aClass.getAnnotation(KotlinInfo.class);

        PackageData data = JavaProtoBufUtil.readPackageDataFrom(kotlinInfo.data());

        Set<String> classNames = collectClassNames(data);
        assertSameElements(Arrays.asList("A", "B", "C"), classNames);

        Set<String> callableNames = collectCallableNames(data);
        assertSameElements(Arrays.asList("foo", "bar", "C"), callableNames);
    }

    @NotNull
    private static Set<String> collectCallableNames(@NotNull PackageData data) {
        Set<String> callableNames = new HashSet<String>();
        List<ProtoBuf.Callable> list = data.getPackageProto().getMemberList();
        for (ProtoBuf.Callable callable : list) {
            callableNames.add(data.getNameResolver().getName(callable.getName()).asString());
        }
        return callableNames;
    }

    @NotNull
    private static Set<String> collectClassNames(@NotNull PackageData data) {
        Set<String> classNames = new HashSet<String>();
        for (int name : data.getPackageProto().getClassNameList()) {
            classNames.add(data.getNameResolver().getName(name).asString());
        }
        return classNames;
    }
}
