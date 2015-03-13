trait T {
    fun foo(): Int
}

trait U: T

fun test(t: T): Int {
    return if (t is U)
        <selection>t.foo()</selection> + 1
    else t.foo()
}