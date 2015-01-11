/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
    public void pathRoot() {
        List<FqName> path = new FqName("").path();
        Assert.assertEquals(1, path.size());
        Assert.assertEquals("", path.get(0).asString());
    }

    @Test
    public void pathLevel1() {
        List<FqName> path = new FqName("org").path();
        Assert.assertEquals(2, path.size());
        Assert.assertEquals("", path.get(0).asString());
        Assert.assertEquals("org", path.get(1).asString());
        Assert.assertEquals("org", path.get(1).shortName().asString());
        Assert.assertEquals("", path.get(1).parent().asString());
    }

    @Test
    public void pathLevel2() {
        List<FqName> path = new FqName("org.jetbrains").path();
        Assert.assertEquals(3, path.size());
        Assert.assertEquals("", path.get(0).asString());
        Assert.assertEquals("org", path.get(1).asString());
        Assert.assertEquals("org", path.get(1).shortName().asString());
        Assert.assertEquals("", path.get(1).parent().asString());
        Assert.assertEquals("org.jetbrains", path.get(2).asString());
        Assert.assertEquals("jetbrains", path.get(2).shortName().asString());
        Assert.assertEquals("org", path.get(2).parent().asString());
    }

    @Test
    public void pathLevel3() {
        List<FqName> path = new FqName("org.jetbrains.kotlin").path();
        Assert.assertEquals(4, path.size());
        Assert.assertEquals("", path.get(0).asString());
        Assert.assertEquals("org", path.get(1).asString());
        Assert.assertEquals("org", path.get(1).shortName().asString());
        Assert.assertEquals("", path.get(1).parent().asString());
        Assert.assertEquals("org.jetbrains", path.get(2).asString());
        Assert.assertEquals("jetbrains", path.get(2).shortName().asString());
        Assert.assertEquals("org", path.get(2).parent().asString());
        Assert.assertEquals("org.jetbrains.kotlin", path.get(3).asString());
        Assert.assertEquals("kotlin", path.get(3).shortName().asString());
        Assert.assertEquals("org.jetbrains", path.get(3).parent().asString());
    }

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
        Assert.assertTrue(NamePackage.isValidJavaFqName(""));
        Assert.assertTrue(NamePackage.isValidJavaFqName("a"));
        Assert.assertTrue(NamePackage.isValidJavaFqName("1"));
        Assert.assertTrue(NamePackage.isValidJavaFqName("a.a"));
        Assert.assertTrue(NamePackage.isValidJavaFqName("org.jetbrains"));
        Assert.assertTrue(NamePackage.isValidJavaFqName("$"));
        Assert.assertTrue(NamePackage.isValidJavaFqName("org.A$B"));

        Assert.assertFalse(NamePackage.isValidJavaFqName("."));
        Assert.assertFalse(NamePackage.isValidJavaFqName(".."));
        Assert.assertFalse(NamePackage.isValidJavaFqName("a."));
        Assert.assertFalse(NamePackage.isValidJavaFqName(".a"));
        Assert.assertFalse(NamePackage.isValidJavaFqName("a..b"));
        Assert.assertFalse(NamePackage.isValidJavaFqName("a.b.."));
        Assert.assertFalse(NamePackage.isValidJavaFqName("a.b."));
        Assert.assertFalse(NamePackage.isValidJavaFqName("a.b...)"));
        Assert.assertFalse(NamePackage.isValidJavaFqName("a.b.<special>"));
    }
}
