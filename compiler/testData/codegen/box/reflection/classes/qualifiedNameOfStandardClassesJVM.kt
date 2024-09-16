// TARGET_BACKEND: JVM
// WITH_STDLIB

import kotlin.test.assertEquals

fun box(): String {
    assertEquals("kotlin.Array", Array<Any>::class.qualifiedName)
    assertEquals("kotlin.Array", Array<IntArray>::class.qualifiedName)
    assertEquals("kotlin.Array", Array<Array<String>>::class.qualifiedName)

    // TODO: KT-11754
    assertEquals("kotlin.collections.List", MutableList::class.qualifiedName)
    assertEquals("kotlin.collections.Map.Entry", MutableMap.MutableEntry::class.qualifiedName)

    assertEquals("kotlin.jvm.functions.FunctionN", Function42::class.qualifiedName)

    return "OK"
}
