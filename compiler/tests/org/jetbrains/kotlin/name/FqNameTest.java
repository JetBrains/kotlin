/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.name;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FqNameTest {
    @Test
    public void pathSegments() {
        Assertions.assertEquals(new ArrayList<Name>(), new FqName("").pathSegments());

        for (String name : new String[] {"org", "org.jetbrains", "org.jetbrains.kotlin"}) {
            List<Name> segments = new FqName(name).pathSegments();
            List<String> segmentsStrings = new ArrayList<>();
            for (Name segment : segments) {
                segmentsStrings.add(segment.asString());
            }
            Assertions.assertEquals(Arrays.asList(name.split("\\.")), segmentsStrings);
        }
    }

    @Test
    public void safeUnsafe() {
        FqName fqName = new FqName("com.yandex");
        Assertions.assertSame(fqName, fqName.toUnsafe().toSafe());
    }

    @Test
    public void unsafeSafe() {
        FqNameUnsafe fqName = new FqNameUnsafe("ru.yandex");
        Assertions.assertSame(fqName, fqName.toSafe().toUnsafe());
    }

    @Test
    public void isValidJavaFqName() {
        Assertions.assertTrue(FqNamesUtilKt.isValidJavaFqName(""));
        Assertions.assertTrue(FqNamesUtilKt.isValidJavaFqName("a"));
        Assertions.assertTrue(FqNamesUtilKt.isValidJavaFqName("a1"));
        Assertions.assertTrue(FqNamesUtilKt.isValidJavaFqName("a.a"));
        Assertions.assertTrue(FqNamesUtilKt.isValidJavaFqName("org.jetbrains"));
        Assertions.assertTrue(FqNamesUtilKt.isValidJavaFqName("$"));
        Assertions.assertTrue(FqNamesUtilKt.isValidJavaFqName("_"));
        Assertions.assertTrue(FqNamesUtilKt.isValidJavaFqName("org.A$B"));
        Assertions.assertTrue(FqNamesUtilKt.isValidJavaFqName("晴れの日"));

        Assertions.assertFalse(FqNamesUtilKt.isValidJavaFqName(" "));
        Assertions.assertFalse(FqNamesUtilKt.isValidJavaFqName(" a"));
        Assertions.assertFalse(FqNamesUtilKt.isValidJavaFqName("a "));
        Assertions.assertFalse(FqNamesUtilKt.isValidJavaFqName("1"));
        Assertions.assertFalse(FqNamesUtilKt.isValidJavaFqName("1a"));
        Assertions.assertFalse(FqNamesUtilKt.isValidJavaFqName("."));
        Assertions.assertFalse(FqNamesUtilKt.isValidJavaFqName(".."));
        Assertions.assertFalse(FqNamesUtilKt.isValidJavaFqName("a."));
        Assertions.assertFalse(FqNamesUtilKt.isValidJavaFqName(".a"));
        Assertions.assertFalse(FqNamesUtilKt.isValidJavaFqName("a..b"));
        Assertions.assertFalse(FqNamesUtilKt.isValidJavaFqName("a.b.."));
        Assertions.assertFalse(FqNamesUtilKt.isValidJavaFqName("a.b."));
        Assertions.assertFalse(FqNamesUtilKt.isValidJavaFqName("a.b...)"));
        Assertions.assertFalse(FqNamesUtilKt.isValidJavaFqName("a.b.<special>"));
        Assertions.assertFalse(FqNamesUtilKt.isValidJavaFqName("😀"));
    }
}
