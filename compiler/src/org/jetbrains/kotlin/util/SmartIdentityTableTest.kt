/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.utils.SmartIdentityTable
import org.junit.Assert
import org.junit.Test

class SmartIdentityTableTest {
    class Key(val number: Int) {
        override fun equals(other: Any?): Boolean {
            Assert.fail("equals Should not be called")
            return false
        }

        override fun hashCode(): Int {
            Assert.fail("equals Should not be called")
            return 0
        }
    }

    class Value(val number: Int) {
        override fun equals(other: Any?): Boolean {
            Assert.fail("equals Should not be called")
            return false
        }

        override fun hashCode(): Int {
            Assert.fail("equals Should not be called")
            return 0
        }
    }

    @Test
    fun basicTest() {
        val key1 = Key(1)
        val key2 = Key(2)
        val key3 = Key(3)
        val val1 = Value(1)
        val val2 = Value(2)
        val val3 = Value(3)

        val table = SmartIdentityTable<Key, Value>()

        // insert two keys and ensure that those are present
        table[key1] = val1
        table[key2] = val2

        Assert.assertEquals(2, table.size)
        Assert.assertTrue(table[key1] === val1)
        Assert.assertTrue(table[key2] === val2)

        // replace existing key's value
        table[key1] = val3

        // expect size to stay the same
        Assert.assertEquals(2, table.size)

        // values should be updated for key1 and same for other key
        Assert.assertTrue(table[key1] === val3)
        Assert.assertTrue(table[key2] === val2)

        // add a new key with existing value
        table[key3] = val2

        // new key should be added and existing keys should maintain their values
        Assert.assertEquals(3, table.size)
        Assert.assertTrue(table[key1] === val3)
        Assert.assertTrue(table[key2] === val2)
        Assert.assertTrue(table[key3] === val2)

        // create a key that has the same contents but a different reference identity, it should not be found in the table.
        val secondKey1 = Key(1)
        Assert.assertTrue(table[secondKey1] === null)
    }

    @Test
    fun growToMapTest() {
        val table = SmartIdentityTable<Key, Value>()

        // insert enough data to trigger conversion of the Table to use a Map
        val keys = mutableListOf<Key>()
        for (i in 0 until 15) {
            val key = Key(i)
            val value = Value(i)
            keys.add(key)
            table[key] = value
        }

        Assert.assertEquals(15, table.size)
        for (key in keys) {
            val value = table[key]
            Assert.assertNotNull(value)
            Assert.assertEquals(key.number, table[key]!!.number)
        }
    }

    @Test
    fun getOrCreateTest() {
        val table = SmartIdentityTable<Key, Value>()
        val f = { Value(5) }
        table.getOrCreate(Key(5), f)
    }
}
