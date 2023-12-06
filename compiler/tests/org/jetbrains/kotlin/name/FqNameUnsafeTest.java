/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FqNameUnsafeTest {
    @Test
    public void pathSegments() {
        Assert.assertEquals(new ArrayList<Name>(), new FqNameUnsafe("").pathSegments());

        for (String name : new String[] { "org", "org.jetbrains", "org.jetbrains.kotlin" }) {
            List<Name> segments = new FqNameUnsafe(name).pathSegments();
            List<String> segmentsStrings = new ArrayList<>();
            for (Name segment : segments) {
                segmentsStrings.add(segment.asString());
            }
            Assert.assertEquals(Arrays.asList(name.split("\\.")), segmentsStrings);
        }
    }

    @Test
    public void startsWithName() {
        Assert.assertTrue(new FqNameUnsafe("abc.def").startsWith(Name.identifier("abc")));
        Assert.assertTrue(new FqNameUnsafe("abc").startsWith(Name.identifier("abc")));
        Assert.assertTrue(new FqNameUnsafe("abc.").startsWith(Name.identifier("abc")));
        Assert.assertTrue(new FqNameUnsafe(".abc").startsWith(Name.identifier("")));

        Assert.assertFalse(new FqNameUnsafe("").startsWith(Name.identifier("")));
        Assert.assertFalse(new FqNameUnsafe("").startsWith(Name.identifier("id")));

        Assert.assertFalse(new FqNameUnsafe("segment").startsWith(Name.identifier("")));
        Assert.assertFalse(new FqNameUnsafe("abc.").startsWith(Name.identifier("abc.")));
        Assert.assertFalse(new FqNameUnsafe(".abc").startsWith(Name.identifier("abc")));
        Assert.assertFalse(new FqNameUnsafe(".abc").startsWith(Name.identifier(".")));
        Assert.assertFalse(new FqNameUnsafe(".abc").startsWith(Name.identifier(".abc")));
        Assert.assertFalse(new FqNameUnsafe("abc.def").startsWith(Name.identifier("abc.def")));
        Assert.assertFalse(new FqNameUnsafe("abcdef").startsWith(Name.identifier("abc")));
        Assert.assertFalse(new FqNameUnsafe("abc").startsWith(Name.identifier("abcdef")));
        Assert.assertFalse(new FqNameUnsafe("abc.xyz").startsWith(Name.identifier("abcdef")));
    }
}
