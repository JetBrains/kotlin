// WITH_STDLIB
// FULL_JDK

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: expressions, built-in-types-and-their-semantics, kotlin.nothing-1 -> paragraph 1 -> sentence 1
 * NUMBER: 7
 * DESCRIPTION: check kotlin.Nothing type
 */

import java.util.*

fun box() : String{
    val deque1 = ArrayDeque<Any>()
    val deque2 = ArrayDeque<Any>()
    deque1.add { throw IllegalArgumentException() }
    deque1.add { throw NullPointerException() }
    deque1.add { TODO() }
    move(deque1, deque2)
    val v = deque2.first as () -> Nothing
    try {
        v.invoke()
    } catch (e: NotImplementedError) {
        return "OK"
    }
    return "NOK"
}

fun <T> move(from: ArrayDeque<out T>, to: ArrayDeque<in T>) {
    to.add(from.last())
}
