// ISSUE: KT-68575

fun <T, R> foo(t: T, block: (something: T) -> R): R = TODO()

val x1 = foo("") { x -> x }
val x2 = foo("") { it }

val expectedType1: (s: String) -> Int = { it.length }
val expectedType2: (s: String) -> Int = { x -> x.length }

fun bar() = foo("") { x -> x }

fun myMain() {
    val y = foo("") { x -> x }

    fun baz() = foo("") { x -> x }
}
