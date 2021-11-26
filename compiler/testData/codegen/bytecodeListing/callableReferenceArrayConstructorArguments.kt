// WITH_STDLIB

fun foo1(x: Int) = x
fun foo2(vararg x: Int) = x[0]
fun Int.foo3() = this
fun IntArray.foo4(x: Int) = this[x]
val Int.foo5 get() = this

fun test() {
    // None of this should create any Function1 implementations because IntArray is inline.
    IntArray(1, ::foo1)
    IntArray(1, ::foo2)
    IntArray(1, Int::foo3)
    IntArray(1, intArrayOf(0)::foo4)
    IntArray(1, Int::foo5)
}
