// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: expressions, built-in-types-and-their-semantics, kotlin.nothing-1 -> paragraph 1 -> sentence 1
 * NUMBER: 5
 * DESCRIPTION: check kotlin.Nothing type
 */


fun box(): String {
    val person = Person("Elvis")
    person.name
    try {
        person.name = null
        person.name ?: throwException("Name is required")
    } catch (e: IllegalArgumentException) {
        return "OK"
    }
    return "NOK"
}

class Person(var name: String?) {}

fun throwException(m: String): Nothing {
    throw  IllegalArgumentException(m)
}

