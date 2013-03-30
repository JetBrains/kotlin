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

import com.google.common.collect.Lists;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.util.QualifiedNamesUtil;
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
        Assert.assertEquals("", path.get(0).getFqName());
    }

    @Test
    public void pathLevel1() {
        List<FqName> path = new FqName("com").path();
        Assert.assertEquals(2, path.size());
        Assert.assertEquals("", path.get(0).getFqName());
        Assert.assertEquals("com", path.get(1).getFqName());
        Assert.assertEquals("com", path.get(1).shortName().getName());
        Assert.assertEquals("", path.get(1).parent().getFqName());
    }

    @Test
    public void pathLevel2() {
        List<FqName> path = new FqName("com.jetbrains").path();
        Assert.assertEquals(3, path.size());
        Assert.assertEquals("", path.get(0).getFqName());
        Assert.assertEquals("com", path.get(1).getFqName());
        Assert.assertEquals("com", path.get(1).shortName().getName());
        Assert.assertEquals("", path.get(1).parent().getFqName());
        Assert.assertEquals("com.jetbrains", path.get(2).getFqName());
        Assert.assertEquals("jetbrains", path.get(2).shortName().getName());
        Assert.assertEquals("com", path.get(2).parent().getFqName());
    }

    @Test
    public void pathLevel3() {
        List<FqName> path = new FqName("com.jetbrains.jet").path();
        Assert.assertEquals(4, path.size());
        Assert.assertEquals("", path.get(0).getFqName());
        Assert.assertEquals("com", path.get(1).getFqName());
        Assert.assertEquals("com", path.get(1).shortName().getName());
        Assert.assertEquals("", path.get(1).parent().getFqName());
        Assert.assertEquals("com.jetbrains", path.get(2).getFqName());
        Assert.assertEquals("jetbrains", path.get(2).shortName().getName());
        Assert.assertEquals("com", path.get(2).parent().getFqName());
        Assert.assertEquals("com.jetbrains.jet", path.get(3).getFqName());
        Assert.assertEquals("jet", path.get(3).shortName().getName());
        Assert.assertEquals("com.jetbrains", path.get(3).parent().getFqName());
    }

    @Test
    public void pathSegments() {
        Assert.assertEquals(Lists.newArrayList(), new FqName("").pathSegments());

        for (String name : new String[] { "com", "com.jetbrains", "com.jetbrains.jet" }) {
            List<Name> segments = new FqName(name).pathSegments();
            List<String> segmentsStrings = new ArrayList<String>();
            for (Name segment : segments) {
                segmentsStrings.add(segment.getName());
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
        Assert.assertTrue(QualifiedNamesUtil.isValidJavaFqName(""));
        Assert.assertTrue(QualifiedNamesUtil.isValidJavaFqName("a"));
        Assert.assertTrue(QualifiedNamesUtil.isValidJavaFqName("1"));
        Assert.assertTrue(QualifiedNamesUtil.isValidJavaFqName("a.a"));
        Assert.assertTrue(QualifiedNamesUtil.isValidJavaFqName("org.jetbrains"));
        Assert.assertTrue(QualifiedNamesUtil.isValidJavaFqName("$"));
        Assert.assertTrue(QualifiedNamesUtil.isValidJavaFqName("org.A$B"));

        Assert.assertFalse(QualifiedNamesUtil.isValidJavaFqName("."));
        Assert.assertFalse(QualifiedNamesUtil.isValidJavaFqName(".."));
        Assert.assertFalse(QualifiedNamesUtil.isValidJavaFqName("a."));
        Assert.assertFalse(QualifiedNamesUtil.isValidJavaFqName(".a"));
        Assert.assertFalse(QualifiedNamesUtil.isValidJavaFqName("a..b"));
        Assert.assertFalse(QualifiedNamesUtil.isValidJavaFqName("a.b.."));
        Assert.assertFalse(QualifiedNamesUtil.isValidJavaFqName("a.b."));
        Assert.assertFalse(QualifiedNamesUtil.isValidJavaFqName("a.b...)"));
        Assert.assertFalse(QualifiedNamesUtil.isValidJavaFqName("a.b.<special>"));
    }
}
