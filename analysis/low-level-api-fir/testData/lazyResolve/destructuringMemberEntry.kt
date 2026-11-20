// KT-90199
data class MyPair(val i: Int, val b: Int)

class A {
    val pair = MyPair(1, 2)

    val (fi<caret>rst, last) = pair
}
