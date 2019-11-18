// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    assertEquals("kotlin.Any", Any::class.qualifiedName)
    assertEquals("kotlin.String", String::class.qualifiedName)
    assertEquals("kotlin.CharSequence", CharSequence::class.qualifiedName)
    assertEquals("kotlin.Number", Number::class.qualifiedName)
    assertEquals("kotlin.Int", Int::class.qualifiedName)
    assertEquals("kotlin.Long", Long::class.qualifiedName)

    assertEquals("kotlin.Array", Array<Any>::class.qualifiedName)
    assertEquals("kotlin.Array", Array<IntArray>::class.qualifiedName)
    assertEquals("kotlin.Array", Array<Array<String>>::class.qualifiedName)

    assertEquals("kotlin.IntArray", IntArray::class.qualifiedName)
    assertEquals("kotlin.DoubleArray", DoubleArray::class.qualifiedName)

    assertEquals("kotlin.Int.Companion", Int.Companion::class.qualifiedName)
    assertEquals("kotlin.Double.Companion", Double.Companion::class.qualifiedName)
    assertEquals("kotlin.Char.Companion", Char.Companion::class.qualifiedName)

    assertEquals("kotlin.ranges.IntRange", IntRange::class.qualifiedName)

    assertEquals("kotlin.collections.List", List::class.qualifiedName)
    assertEquals("kotlin.collections.Map.Entry", Map.Entry::class.qualifiedName)

    // TODO: KT-11754
    assertEquals("kotlin.collections.List", MutableList::class.qualifiedName)
    assertEquals("kotlin.collections.Map.Entry", MutableMap.MutableEntry::class.qualifiedName)

    assertEquals("kotlin.Function0", Function0::class.qualifiedName)
    assertEquals("kotlin.Function1", Function1::class.qualifiedName)
    assertEquals("kotlin.Function5", Function5::class.qualifiedName)
    assertEquals("kotlin.jvm.functions.FunctionN", Function42::class.qualifiedName)

    return "OK"
}
