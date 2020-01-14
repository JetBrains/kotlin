data class D(val v1: Int, val v2: Int)

fun foo(): D = D(1, 2)

fun test(): Int {
    val <caret>foo = foo()
    return foo.v1 + foo.v2
}