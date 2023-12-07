/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name;

import org.junit.Assert;
import org.junit.Test;

public class FqNameUnsafeTest {
    @Test
    public void startsWithName() {
        Assert.assertTrue(new FqNameUnsafe("abc.def").startsWith(Name.identifier("abc")));
        Assert.assertTrue(new FqNameUnsafe("abc").startsWith(Name.identifier("abc")));
        Assert.assertTrue(new FqNameUnsafe("abc.").startsWith(Name.identifier("abc")));
        Assert.assertTrue(new FqNameUnsafe(".abc").startsWith(Name.identifier("")));

        Assert.assertFalse(new FqNameUnsafe("").startsWith(Name.identifier("")));
        Assert.assertFalse(new FqNameUnsafe("").startsWith(Name.identifier("id")));

        Assert.assertFalse(new FqNameUnsafe("segment").startsWith(Name.identifier("")));
        Assert.assertFalse(new FqNameUnsafe(".abc").startsWith(Name.identifier("abc")));
        Assert.assertFalse(new FqNameUnsafe(".abc").startsWith(Name.identifier("xyz")));
        Assert.assertFalse(new FqNameUnsafe("abcdef").startsWith(Name.identifier("abc")));
        Assert.assertFalse(new FqNameUnsafe("abc").startsWith(Name.identifier("abcdef")));
        Assert.assertFalse(new FqNameUnsafe("abc.xyz").startsWith(Name.identifier("abcdef")));

        // special names
        Assert.assertFalse(new FqNameUnsafe("abc.def").startsWith(Name.special("<abc>")));
        Assert.assertFalse(new FqNameUnsafe("abc").startsWith(Name.special("<abc>")));
        Assert.assertFalse(new FqNameUnsafe("abc.").startsWith(Name.special("<abc>")));
        Assert.assertFalse(new FqNameUnsafe(".abc").startsWith(Name.special("<>")));

        Assert.assertFalse(new FqNameUnsafe("").startsWith(Name.special("<>")));
        Assert.assertFalse(new FqNameUnsafe("").startsWith(Name.special("<id>")));

        Assert.assertFalse(new FqNameUnsafe("segment").startsWith(Name.special("<>")));
        Assert.assertFalse(new FqNameUnsafe(".abc").startsWith(Name.special("<abc>")));
        Assert.assertFalse(new FqNameUnsafe("abcdef").startsWith(Name.special("<abc>")));
        Assert.assertFalse(new FqNameUnsafe("abc").startsWith(Name.special("<abcdef>")));
        Assert.assertFalse(new FqNameUnsafe("abc.xyz").startsWith(Name.special("<abcdef>")));

        Assert.assertTrue(new FqNameUnsafe("<abc>.def").startsWith(Name.special("<abc>")));
        Assert.assertTrue(new FqNameUnsafe("<abc>").startsWith(Name.special("<abc>")));
        Assert.assertTrue(new FqNameUnsafe("<abc>.").startsWith(Name.special("<abc>")));
        Assert.assertTrue(new FqNameUnsafe("<>.abc").startsWith(Name.special("<>")));
    }

    @Test
    public void startsWithFqNameUnsafe() {
        Assert.assertTrue(new FqNameUnsafe("abc.def").startsWith(new FqNameUnsafe("abc.def")));
        Assert.assertTrue(new FqNameUnsafe("abc.def").startsWith(new FqNameUnsafe("abc")));
        Assert.assertTrue(new FqNameUnsafe("abc").startsWith(new FqNameUnsafe("abc")));
        Assert.assertTrue(new FqNameUnsafe("abc.").startsWith(new FqNameUnsafe("abc.")));
        Assert.assertTrue(new FqNameUnsafe("abc.").startsWith(new FqNameUnsafe("abc")));
        Assert.assertTrue(new FqNameUnsafe("abc.def.").startsWith(new FqNameUnsafe("abc")));
        Assert.assertTrue(new FqNameUnsafe("abc.def.").startsWith(new FqNameUnsafe("abc.def")));
        Assert.assertTrue(new FqNameUnsafe("abc.def.").startsWith(new FqNameUnsafe("abc.def.")));
        Assert.assertTrue(new FqNameUnsafe(".abc").startsWith(new FqNameUnsafe("")));

        Assert.assertFalse(new FqNameUnsafe("").startsWith(new FqNameUnsafe("")));
        Assert.assertFalse(new FqNameUnsafe("").startsWith(new FqNameUnsafe("id")));

        Assert.assertFalse(new FqNameUnsafe("segment").startsWith(new FqNameUnsafe("")));
        Assert.assertFalse(new FqNameUnsafe(".abc").startsWith(new FqNameUnsafe("abc")));
        Assert.assertFalse(new FqNameUnsafe(".abc").startsWith(new FqNameUnsafe("xyz")));
        Assert.assertFalse(new FqNameUnsafe("abcdef").startsWith(new FqNameUnsafe("abc")));
        Assert.assertFalse(new FqNameUnsafe("abc").startsWith(new FqNameUnsafe("abcdef")));
        Assert.assertFalse(new FqNameUnsafe("abc").startsWith(new FqNameUnsafe("abc.")));
        Assert.assertFalse(new FqNameUnsafe("abc.def").startsWith(new FqNameUnsafe("abc.")));
        Assert.assertFalse(new FqNameUnsafe("abc.def").startsWith(new FqNameUnsafe("abcdef")));
        Assert.assertFalse(new FqNameUnsafe("abc.def").startsWith(new FqNameUnsafe("abcxyz")));
        Assert.assertFalse(new FqNameUnsafe("abc.def").startsWith(new FqNameUnsafe("abc.xyz")));

        // special names
        Assert.assertFalse(new FqNameUnsafe("abc.def").startsWith(new FqNameUnsafe("<abc>")));
        Assert.assertFalse(new FqNameUnsafe("abc").startsWith(new FqNameUnsafe("<abc>")));
        Assert.assertFalse(new FqNameUnsafe("abc.").startsWith(new FqNameUnsafe("<abc>")));
        Assert.assertFalse(new FqNameUnsafe(".abc").startsWith(new FqNameUnsafe("<>")));

        Assert.assertFalse(new FqNameUnsafe("").startsWith(new FqNameUnsafe("<>")));
        Assert.assertFalse(new FqNameUnsafe("").startsWith(new FqNameUnsafe("<id>")));

        Assert.assertFalse(new FqNameUnsafe("segment").startsWith(new FqNameUnsafe("<>")));
        Assert.assertFalse(new FqNameUnsafe(".abc").startsWith(new FqNameUnsafe("<abc>")));
        Assert.assertFalse(new FqNameUnsafe("abcdef").startsWith(new FqNameUnsafe("<abc>")));
        Assert.assertFalse(new FqNameUnsafe("abc").startsWith(new FqNameUnsafe("<abcdef>")));
        Assert.assertFalse(new FqNameUnsafe("abc.xyz").startsWith(new FqNameUnsafe("<abcdef>")));

        Assert.assertFalse(new FqNameUnsafe("<abc>.def").startsWith(new FqNameUnsafe("<abc>.")));
        Assert.assertTrue(new FqNameUnsafe("<abc>").startsWith(new FqNameUnsafe("<abc>")));
        Assert.assertTrue(new FqNameUnsafe("<abc>.").startsWith(new FqNameUnsafe("<abc>")));
        Assert.assertTrue(new FqNameUnsafe("<abc.>").startsWith(new FqNameUnsafe("<abc.>")));
        Assert.assertTrue(new FqNameUnsafe("<>.abc").startsWith(new FqNameUnsafe("<>")));
    }
}
