// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt

package test

inline fun foo(crossinline f: (String) -> Unit): Any = bar { f(it[0]) }

fun bar(f: (Array<String>) -> Unit): Any = f

// FILE: 2.kt

import test.*

private fun getBar() = bar { it.size }

private fun getFoo() = foo { it.length }

private fun getFoo2() = foo { it.length }

fun box(): String {
    val a = getFoo()
    val b = getFoo()
    if (a !== b) {
        return "First check failed"
    }
    val c = getBar()
    val d = getBar()
    if (c !== d) {
        return "Second check failed"
    }
    val e = getFoo()
    val f = getFoo2()
    if (e === f) {
        return "Third check failed"
    }
    return "OK"
}
