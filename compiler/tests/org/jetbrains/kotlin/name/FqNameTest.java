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

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FqNameTest {
    @Test
    public void pathSegments() {
        Assert.assertEquals(new ArrayList<Name>(), new FqName("").pathSegments());

        for (String name : new String[] { "org", "org.jetbrains", "org.jetbrains.kotlin" }) {
            List<Name> segments = new FqName(name).pathSegments();
            List<String> segmentsStrings = new ArrayList<String>();
            for (Name segment : segments) {
                segmentsStrings.add(segment.asString());
            }
            Assert.assertEquals(Arrays.asList(name.split("\\.")), segmentsStrings);
        }
    }

    @Test
    public void safeUnsafe() {
        FqName fqName = new FqName("com.yandex");
        Assert.assertSame(fqName, fqName.toUnsafe().toSafe());
    }

    @Test
    public void unsafeSafe() {
        FqNameUnsafe fqName = new FqNameUnsafe("ru.yandex");
        Assert.assertSame(fqName, fqName.toSafe().toUnsafe());
    }

    @Test
    public void isValidJavaFqName() {
        Assert.assertTrue(FqNamesUtilKt.isValidJavaFqName(""));
        Assert.assertTrue(FqNamesUtilKt.isValidJavaFqName("a"));
        Assert.assertTrue(FqNamesUtilKt.isValidJavaFqName("1"));
        Assert.assertTrue(FqNamesUtilKt.isValidJavaFqName("a.a"));
        Assert.assertTrue(FqNamesUtilKt.isValidJavaFqName("org.jetbrains"));
        Assert.assertTrue(FqNamesUtilKt.isValidJavaFqName("$"));
        Assert.assertTrue(FqNamesUtilKt.isValidJavaFqName("org.A$B"));

        Assert.assertFalse(FqNamesUtilKt.isValidJavaFqName("."));
        Assert.assertFalse(FqNamesUtilKt.isValidJavaFqName(".."));
        Assert.assertFalse(FqNamesUtilKt.isValidJavaFqName("a."));
        Assert.assertFalse(FqNamesUtilKt.isValidJavaFqName(".a"));
        Assert.assertFalse(FqNamesUtilKt.isValidJavaFqName("a..b"));
        Assert.assertFalse(FqNamesUtilKt.isValidJavaFqName("a.b.."));
        Assert.assertFalse(FqNamesUtilKt.isValidJavaFqName("a.b."));
        Assert.assertFalse(FqNamesUtilKt.isValidJavaFqName("a.b...)"));
        Assert.assertFalse(FqNamesUtilKt.isValidJavaFqName("a.b.<special>"));
    }
}
