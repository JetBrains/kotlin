// KT-74654
// KT-90199: annotations on destructuring entries have no FIR yet
annotation class Ann(val s: String)

const val x = "str"

class C {
    val (@A<caret>nn(x) a, b) = 1 to 2
}
