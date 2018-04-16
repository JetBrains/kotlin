/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

@file:Suppress("UNUSED")

package stdlib

fun <K, V> isEmpty(map: Map<K, V>) = map.isEmpty()

fun <K, V> getKeysAsSet(map: Map<K, V>) = map.keys
fun <K, V> getKeysAsList(map: Map<K, V>) = map.keys.toList()

fun <K, V> toMutableMap(map: HashMap<K, V>) = map.toMutableMap()

fun <E> getFirstElement(collection: Collection<E>) = collection.first()

class GenericExtensionClass<K, out V, out T : Map<K, V>> (private val holder: T?) {
    fun getFirstKey(): K? = holder?.entries?.first()?.key

    fun getFirstValue() : V? {
        holder?.entries?.forEach { e -> println("KEY: ${e.key}  VALUE: ${e.value}") }
        return holder?.entries?.first()?.value
    }
}

fun <K, V> createPair():
        Pair<LinkedHashMap<K, V>, GenericExtensionClass<K, V, Map<K, V>>> {
    val l = createLinkedMap<K, V>()
    val g = GenericExtensionClass(l)
    return Pair(l, g)
}

fun <K, V> createLinkedMap() = linkedMapOf<K, V>()

fun createTypedMutableMap() = linkedMapOf<Int, String>()

fun addSomeElementsToMap(map: MutableMap<String, Int>) {
    map.put(key = "XYZ", value = 321)
    map.put(key = "TMP", value = 451)
}

fun list(vararg elements: Any?): Any = listOf(*elements)
fun set(vararg elements: Any?): Any = setOf(*elements)
fun map(vararg keysAndValues: Any?): Any = mutableMapOf<Any?, Any?>().apply {
    (0 until keysAndValues.size step 2).forEach {index ->
        this[keysAndValues[index]] = keysAndValues[index + 1]
    }
}

fun emptyMutableList(): Any = mutableListOf<Any?>()
fun emptyMutableSet(): Any = mutableSetOf<Any?>()
fun emptyMutableMap(): Any = mutableMapOf<Any?, Any?>()

data class TripleVals<T>(val first: T, val second: T, val third: T)

data class TripleVars<T>(var first: T, var second: T, var third: T) {
    override fun toString(): String {
        return "[$first, $second, $third]"
    }
}