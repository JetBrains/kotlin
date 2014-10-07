class Expr {}
class Num : Expr() {
    fun testing() {}
}

fun eval(e : Expr) {
    if (e is Num) {
        return e.<caret>()
    }
}

// EXIST: testing