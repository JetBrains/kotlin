// KT-74654
annotation class Ann(val s: String)

const val x = "str"

class C {
    @Ann(<caret>x)
    val (a, b) = 1 to 2
}
