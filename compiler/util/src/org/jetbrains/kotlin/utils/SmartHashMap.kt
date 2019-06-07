/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

const val ARRAY_UNTIL_SIZE = 10

/**
 * [SmartHashMap] is a Map implementation that uses reference identity for keys.
 * It uses 2 arrays to store keys & values until the number of entries stored is larger than 10.
 * At that point it switches to using a HashMap and stays that way until [clear] is called.
 * It does not auto convert back if the number of entries decreases through [remove].
 */
class SmartHashMap<K, V>() : MutableMap<K, V> {

    private var keysArray: MutableList<K>? = arrayListOf()
    private var valuesArray: MutableList<V>? = arrayListOf()
    private var largeMap: HashMap<K, V>? = null

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() {
            return keysArray?.let {
                val ms = mutableSetOf<MutableMap.MutableEntry<K, V>>()
                for (i in 0 until it.size) {
                    ms.add(Entry(it[i], valuesArray!![i]))
                }
                return ms
            } ?: largeMap!!.entries
        }


    override val keys: MutableSet<K>
        get() {
            return keysArray?.let {
                val ms = mutableSetOf<K>()
                ms.addAll(it)
                return ms

            } ?: largeMap!!.keys
        }

    override val size: Int
        get() = keysArray?.size ?: largeMap!!.size

    override val values: MutableCollection<V>
        get() {
            return valuesArray?.let {
                val ms = mutableSetOf<V>()
                ms.addAll(it)
                return ms

            } ?: largeMap!!.values
        }

    override fun clear() {
        largeMap = null
        keysArray = arrayListOf()
        valuesArray = arrayListOf()
    }

    override fun containsKey(key: K): Boolean = keysArray?.contains(key) ?: largeMap!!.containsKey(key)


    override fun containsValue(value: V): Boolean = valuesArray?.contains(value) ?: largeMap!!.containsValue(value)

    override fun get(key: K): V? {
        return keysArray?.let {

            for (i in 0 until it.size) {
                if (it[i] === key) {
                    return valuesArray!![i]
                }
            }
            return null
        } ?: largeMap!![key]
    }

    override fun isEmpty(): Boolean = keysArray?.isEmpty() ?: false

    override fun put(key: K, value: V): V? {
        val ka = keysArray
        if (ka != null) {
            val va = valuesArray!!
            for (i in 0 until ka.size) {
                if (ka[i] === key) {
                    val tmp = va[i]
                    va[i] = value
                    return tmp
                }
            }
            if (ka.size < ARRAY_UNTIL_SIZE) {
                ka.add(key)
                va.add(value)
                return null
            }
            convertToHashMap()
        }
        return largeMap!!.put(key, value)
    }

    override fun putAll(from: Map<out K, V>) {
        val ka = keysArray
        if (ka != null) {
            if (ka.size + from.size < ARRAY_UNTIL_SIZE) {
                for (entry in from) {
                    put(entry.key, entry.value)
                }
                return
            }
            convertToHashMap()
        }
        largeMap!!.putAll(from)
    }

    override fun remove(key: K): V? {
        val ka = keysArray
        if (ka != null) {
            val va = valuesArray!!
            for (i in 0 until ka.size) {
                if (ka[i] == key) {
                    val tmp = va[i]
                    ka.removeAt(i)
                    va.removeAt(i)
                    return tmp
                }
            }
            return null
        } else {
            return largeMap!!.remove(key)
        }

    }

    private fun convertToHashMap() {
        val map = HashMap<K, V>()
        val ka = keysArray!!
        val va = valuesArray!!
        for (i in 0 until ka.size) {
            map.put(ka[i], va[i])
        }
        largeMap = map
        keysArray = null
        valuesArray = null
    }

    private class Entry<K, V>(override val key: K, override val value: V) : MutableMap.MutableEntry<K, V> {
        override fun setValue(newValue: V): V = throw UnsupportedOperationException("This Entry is not mutable.")
    }
}