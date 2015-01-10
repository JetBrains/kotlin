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

package org.jetbrains.jet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.junit.Test;

import static org.jetbrains.jet.lang.resolve.java.PackageClassUtils.getPackageClassName;
import static org.junit.Assert.assertEquals;

public class PackageClassNameTest {

    @Test
    public void testPackageName1() {
        doTest("kotlin", "KotlinPackage", "_DefaultPackage");
    }

    @Test
    public void testPackageName2() {
        doTest("kotlin.io", "IoPackage", "KotlinPackage");
    }

    @Test
    public void testPackageName3() {
        doTest("kotlin.io.foo", "FooPackage", "IoPackage");
    }

    @Test
    public void testPackageName4() {
        doTest("kotlinTest.ioTest", "IoTestPackage", "KotlinTestPackage");
    }

    @Test
    public void testPackageName5() {
        doTest(FqName.ROOT, "_DefaultPackage", null);
    }

    @Test
    public void testPackageName6() {
        doTest(FqName.ROOT.child(Name.identifier("kotlin")), "KotlinPackage", "_DefaultPackage");
    }

    private static void doTest(@NotNull String name, @NotNull String expectedForChild, @Nullable String expectedForParent) {
        doTest(new FqName(name), expectedForChild, expectedForParent);
    }

    private static void doTest(@NotNull FqName name, @NotNull String expectedForChild, @Nullable String expectedForParent) {
        assertEquals("Wrong result for child [" + name + "].", expectedForChild, getPackageClassName(name));
        if (expectedForParent != null) {
            assertEquals("Wrong result for parent [" + name + "].", expectedForParent, getPackageClassName(name.parent()));
        }
    }
}
