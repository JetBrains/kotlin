// "Create secondary constructor" "true"

class CtorPrimary(val f1: Int)

fun construct() {
    val v6 = CtorPrimary(1, 2<caret>)
}