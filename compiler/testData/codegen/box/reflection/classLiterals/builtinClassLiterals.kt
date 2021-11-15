// TARGET_BACKEND: JVM
// WITH_STDLIB

import kotlin.test.assertEquals

fun box(): String {
    assertEquals("Any", Any::class.simpleName)
    assertEquals("String", String::class.simpleName)
    assertEquals("CharSequence", CharSequence::class.simpleName)
    assertEquals("Number", Number::class.simpleName)
    assertEquals("Int", Int::class.simpleName)
    assertEquals("Long", Long::class.simpleName)

    assertEquals("Array", Array<Any>::class.simpleName)
    assertEquals("Array", Array<IntArray>::class.simpleName)
    assertEquals("Array", Array<Array<String>>::class.simpleName)

    assertEquals("IntArray", IntArray::class.simpleName)
    assertEquals("DoubleArray", DoubleArray::class.simpleName)

    assertEquals("Companion", Int.Companion::class.simpleName)
    assertEquals("Companion", Double.Companion::class.simpleName)
    assertEquals("Companion", Char.Companion::class.simpleName)

    assertEquals("IntRange", IntRange::class.simpleName)

    assertEquals("List", List::class.simpleName)
    assertEquals("Entry", Map.Entry::class.simpleName)

    // TODO: KT-11754
    assertEquals("List", MutableList::class.simpleName)
    assertEquals("Entry", MutableMap.MutableEntry::class.simpleName)

    assertEquals("Function0", Function0::class.simpleName)
    assertEquals("Function1", Function1::class.simpleName)
    assertEquals("Function5", Function5::class.simpleName)
    assertEquals("FunctionN", Function42::class.simpleName)

    return "OK"
}
