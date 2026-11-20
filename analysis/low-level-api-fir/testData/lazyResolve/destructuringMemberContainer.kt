// KT-90199
data class MyPair(val i: Int, val b: Int)

class A {
    v<caret>al (first, last) = MyPair(1, 2)
}
