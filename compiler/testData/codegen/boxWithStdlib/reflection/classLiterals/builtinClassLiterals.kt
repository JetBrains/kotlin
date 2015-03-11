import kotlin.test.assertEquals

fun box(): String {
    assertEquals("Any", Any::class.simpleName)
    assertEquals("String", String::class.simpleName)
    assertEquals("CharSequence", CharSequence::class.simpleName)
    assertEquals("Number", Number::class.simpleName)
    assertEquals("Int", Int::class.simpleName)
    assertEquals("Long", Long::class.simpleName)

    assertEquals("Default", Int.Default::class.simpleName)
    assertEquals("Default", Double.Default::class.simpleName)

    assertEquals("IntRange", IntRange::class.simpleName)

    assertEquals("List", List::class.simpleName)

    // TODO: this is wrong but should be fixed
    assertEquals("List", MutableList::class.simpleName)

    return "OK"
}
