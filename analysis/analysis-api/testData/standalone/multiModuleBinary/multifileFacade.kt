// MODULE: lib
// WITH_STDLIB

// FILE: src/my/collections/Maps.kt
@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("MapsKt")

package my.collections

fun <K, V> myEmptyMap(): Map<K, V> = emptyMap()

// FILE: src/my/collections/jvm/MapsJVM.kt
@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("MapsKt")

package my.collections

fun <K, V> myMapOf(vararg pairs: Pair<K, V>): Map<K, V> = mapOf(*pairs)

// MODULE: app(lib)
// MODULE_KIND: Source
// FILE: main.kt

import my.collections.*

fun test() {
    myMap<caret>Of<Int, String>(
        2 to "2",
        4 to "4",
    )
}
