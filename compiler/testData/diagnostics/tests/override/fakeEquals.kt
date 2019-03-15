// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_REFERENCE

class Foo

fun test(a: Foo, b: Foo) {
    // Note that signature matches the 'equals'
    fun equals(x: Any?): Boolean = false
    equals(b)
}