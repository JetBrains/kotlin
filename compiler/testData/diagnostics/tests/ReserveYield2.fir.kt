// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER -UNUSED_EXPRESSION -UNREACHABLE_CODE -UNUSED_VARIABLE -WRONG_ANNOTATION_TARGET -UNUSED_LAMBDA_EXPRESSION
// LANGUAGE: -YieldIsNoMoreReserved

// FILE: 1.kt

annotation class yield

fun bar(p: Int) {
    yield@ p
    `yield`@ p

    @yield() p
    @`yield`() p

    for (yield in 1..5) {

    }
    { yield: Int -> }

    val (yield) = listOf(4)

}

fun <T> listOf(vararg e: T): List<T> = null!!
operator fun <T> List<T>.component1() = get(0)

// FILE: 2.kt
package p3

enum class yield {
    yield
}

fun f1(yield: Int, foo: Int = yield) {}

fun f2(foo: yield) {}

// FILE: 3.kt
package p4

typealias yield = Number

fun <yield: Number> f1() {}
fun <y: yield> f2() {}

// FILE: 4.kt
object X {
    fun yield() {}

    fun test3(yield: Int) {
        X::yield

        yield::toInt
    }
}
