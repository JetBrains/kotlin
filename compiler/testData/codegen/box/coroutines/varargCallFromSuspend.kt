// WITH_RUNTIME

import kotlin.coroutines.*

var failure = false

suspend fun foo() {
    if ((baz(
            bar()
        ))[0] != 5
    ) failure = true

    val baz = baz(
        bar(),
        bar2(),
        bar3()
    )
    if (baz[0] != 5 || baz[1] != 6 || baz[2] != 7) failure = true

    val arrayOf = arrayOf(
        bar(),
        bar2(),
        bar3()
    )
    if (arrayOf[0] != 5 || arrayOf[1] != 6 || arrayOf[2] != 7) failure = true

    val baz1 = baz(
        *arrayOf(
            bar3(),
            bar2(),
            bar()
        )
    )
    if (baz1[0] != 7 || baz1[1] != 6 || baz1[2] != 5) failure = true

    val baz2 = baz(
        *arrayOf(
            bar3(),
            bar2(),
            bar()
        )
    )
    if (baz2[0] != 7 || baz2[1] != 6 || baz2[2] != 5) failure = true

    val baz3 = bar4(
        *baz(
            bar2(),
            bar(),
            bar3()
        )
    )
    if (baz3[0] != 6 || baz3[1] != 5 || baz3[2] != 7) failure = true

    val bah = bah(101, bar(), *intArrayOf(bar3(), bar2()), 8)
    if (bah[0] != 5 || bah[1] != 7 || bah[2] != 6 || bah[3] != 8 || bah[4] != 101) failure = true
}

fun <T> baz(vararg elements: T): Array<out T> = elements

suspend fun bar() = 5
suspend fun bar2() = 6
suspend fun bar3() = 7

suspend fun <T> bar4(vararg elements: T) = elements

fun bah(index: Int, vararg elements: Int): List<Int> = elements.toList() + index

fun box(): String {
    ::foo.startCoroutine(Continuation(EmptyCoroutineContext) { it.getOrThrow() })

    return if (!failure) "OK" else "FAILURE"
}