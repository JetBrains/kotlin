// KT-74654
// KT-89821: annotations on destructuring entries have no FIR yet
annotation class Ann(val s: String)

const val x = "str"

class C {
    val (a, @Ann(<caret>x) b) = 1 to 2
}
