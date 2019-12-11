package a

fun <T> foo(u: T, v: T): T = u

fun test(s: String?) {
    val r: String = foo(s!!, s)
}