// KT-74654
annotation class Ann(val s: String)

const val x = "str"

data class X(val a: Int, val b: Int)

class B {
    @Ann(x)
    v<caret>al (a, b) = X(1, 2)
}
