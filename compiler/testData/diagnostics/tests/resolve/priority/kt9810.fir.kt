// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE
// KT-9810 Local variable vs property from implicit receiver

class A {
    val foo = 2
}

fun test(foo: String) {
    with(A()) {
        val g: String = foo // locals win
    }
}