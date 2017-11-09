// FLOW: OUT

inline fun foo(a: Int, f: (Int) -> Int) = f(a)

fun bar(a: Int): Int = foo(a) { if (it > 0) it else return <caret>0 }

fun test() {
    val x = bar(1)
}