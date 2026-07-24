/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FqNameUnsafeTest {
    @Test
    public void startsWithName() {
        Assertions.assertTrue(new FqNameUnsafe("abc.def").startsWith(Name.identifier("abc")));
        Assertions.assertTrue(new FqNameUnsafe("abc").startsWith(Name.identifier("abc")));
        Assertions.assertTrue(new FqNameUnsafe("abc.").startsWith(Name.identifier("abc")));
        Assertions.assertTrue(new FqNameUnsafe(".abc").startsWith(Name.identifier("")));

        Assertions.assertFalse(new FqNameUnsafe("").startsWith(Name.identifier("")));
        Assertions.assertFalse(new FqNameUnsafe("").startsWith(Name.identifier("id")));

        Assertions.assertFalse(new FqNameUnsafe("segment").startsWith(Name.identifier("")));
        Assertions.assertFalse(new FqNameUnsafe(".abc").startsWith(Name.identifier("abc")));
        Assertions.assertFalse(new FqNameUnsafe(".abc").startsWith(Name.identifier("xyz")));
        Assertions.assertFalse(new FqNameUnsafe("abcdef").startsWith(Name.identifier("abc")));
        Assertions.assertFalse(new FqNameUnsafe("abc").startsWith(Name.identifier("abcdef")));
        Assertions.assertFalse(new FqNameUnsafe("abc.xyz").startsWith(Name.identifier("abcdef")));

        // special names
        Assertions.assertFalse(new FqNameUnsafe("abc.def").startsWith(Name.special("<abc>")));
        Assertions.assertFalse(new FqNameUnsafe("abc").startsWith(Name.special("<abc>")));
        Assertions.assertFalse(new FqNameUnsafe("abc.").startsWith(Name.special("<abc>")));
        Assertions.assertFalse(new FqNameUnsafe(".abc").startsWith(Name.special("<>")));

        Assertions.assertFalse(new FqNameUnsafe("").startsWith(Name.special("<>")));
        Assertions.assertFalse(new FqNameUnsafe("").startsWith(Name.special("<id>")));

        Assertions.assertFalse(new FqNameUnsafe("segment").startsWith(Name.special("<>")));
        Assertions.assertFalse(new FqNameUnsafe(".abc").startsWith(Name.special("<abc>")));
        Assertions.assertFalse(new FqNameUnsafe("abcdef").startsWith(Name.special("<abc>")));
        Assertions.assertFalse(new FqNameUnsafe("abc").startsWith(Name.special("<abcdef>")));
        Assertions.assertFalse(new FqNameUnsafe("abc.xyz").startsWith(Name.special("<abcdef>")));

        Assertions.assertTrue(new FqNameUnsafe("<abc>.def").startsWith(Name.special("<abc>")));
        Assertions.assertTrue(new FqNameUnsafe("<abc>").startsWith(Name.special("<abc>")));
        Assertions.assertTrue(new FqNameUnsafe("<abc>.").startsWith(Name.special("<abc>")));
        Assertions.assertTrue(new FqNameUnsafe("<>.abc").startsWith(Name.special("<>")));
    }

    @Test
    public void startsWithFqNameUnsafe() {
        Assertions.assertTrue(new FqNameUnsafe("abc.def").startsWith(new FqNameUnsafe("abc.def")));
        Assertions.assertTrue(new FqNameUnsafe("abc.def").startsWith(new FqNameUnsafe("abc")));
        Assertions.assertTrue(new FqNameUnsafe("abc").startsWith(new FqNameUnsafe("abc")));
        Assertions.assertTrue(new FqNameUnsafe("abc.").startsWith(new FqNameUnsafe("abc.")));
        Assertions.assertTrue(new FqNameUnsafe("abc.").startsWith(new FqNameUnsafe("abc")));
        Assertions.assertTrue(new FqNameUnsafe("abc.def.").startsWith(new FqNameUnsafe("abc")));
        Assertions.assertTrue(new FqNameUnsafe("abc.def.").startsWith(new FqNameUnsafe("abc.def")));
        Assertions.assertTrue(new FqNameUnsafe("abc.def.").startsWith(new FqNameUnsafe("abc.def.")));
        Assertions.assertTrue(new FqNameUnsafe(".abc").startsWith(new FqNameUnsafe("")));

        Assertions.assertFalse(new FqNameUnsafe("").startsWith(new FqNameUnsafe("")));
        Assertions.assertFalse(new FqNameUnsafe("").startsWith(new FqNameUnsafe("id")));

        Assertions.assertFalse(new FqNameUnsafe("segment").startsWith(new FqNameUnsafe("")));
        Assertions.assertFalse(new FqNameUnsafe(".abc").startsWith(new FqNameUnsafe("abc")));
        Assertions.assertFalse(new FqNameUnsafe(".abc").startsWith(new FqNameUnsafe("xyz")));
        Assertions.assertFalse(new FqNameUnsafe("abcdef").startsWith(new FqNameUnsafe("abc")));
        Assertions.assertFalse(new FqNameUnsafe("abc").startsWith(new FqNameUnsafe("abcdef")));
        Assertions.assertFalse(new FqNameUnsafe("abc").startsWith(new FqNameUnsafe("abc.")));
        Assertions.assertFalse(new FqNameUnsafe("abc.def").startsWith(new FqNameUnsafe("abc.")));
        Assertions.assertFalse(new FqNameUnsafe("abc.def").startsWith(new FqNameUnsafe("abcdef")));
        Assertions.assertFalse(new FqNameUnsafe("abc.def").startsWith(new FqNameUnsafe("abcxyz")));
        Assertions.assertFalse(new FqNameUnsafe("abc.def").startsWith(new FqNameUnsafe("abc.xyz")));

        // special names
        Assertions.assertFalse(new FqNameUnsafe("abc.def").startsWith(new FqNameUnsafe("<abc>")));
        Assertions.assertFalse(new FqNameUnsafe("abc").startsWith(new FqNameUnsafe("<abc>")));
        Assertions.assertFalse(new FqNameUnsafe("abc.").startsWith(new FqNameUnsafe("<abc>")));
        Assertions.assertFalse(new FqNameUnsafe(".abc").startsWith(new FqNameUnsafe("<>")));

        Assertions.assertFalse(new FqNameUnsafe("").startsWith(new FqNameUnsafe("<>")));
        Assertions.assertFalse(new FqNameUnsafe("").startsWith(new FqNameUnsafe("<id>")));

        Assertions.assertFalse(new FqNameUnsafe("segment").startsWith(new FqNameUnsafe("<>")));
        Assertions.assertFalse(new FqNameUnsafe(".abc").startsWith(new FqNameUnsafe("<abc>")));
        Assertions.assertFalse(new FqNameUnsafe("abcdef").startsWith(new FqNameUnsafe("<abc>")));
        Assertions.assertFalse(new FqNameUnsafe("abc").startsWith(new FqNameUnsafe("<abcdef>")));
        Assertions.assertFalse(new FqNameUnsafe("abc.xyz").startsWith(new FqNameUnsafe("<abcdef>")));

        Assertions.assertFalse(new FqNameUnsafe("<abc>.def").startsWith(new FqNameUnsafe("<abc>.")));
        Assertions.assertTrue(new FqNameUnsafe("<abc>").startsWith(new FqNameUnsafe("<abc>")));
        Assertions.assertTrue(new FqNameUnsafe("<abc>.").startsWith(new FqNameUnsafe("<abc>")));
        Assertions.assertTrue(new FqNameUnsafe("<abc.>").startsWith(new FqNameUnsafe("<abc.>")));
        Assertions.assertTrue(new FqNameUnsafe("<>.abc").startsWith(new FqNameUnsafe("<>")));
    }
}
