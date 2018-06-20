// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

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

    assertEquals("Companion", Int.Companion::class.simpleName)
    assertEquals("Companion", Double.Companion::class.simpleName)
    assertEquals("Companion", Char.Companion::class.simpleName)

    assertEquals("IntRange", IntRange::class.simpleName)

    assertEquals("List", List::class.simpleName)

    // TODO: this is wrong but should be fixed
    assertEquals("List", MutableList::class.simpleName)

    return "OK"
}
