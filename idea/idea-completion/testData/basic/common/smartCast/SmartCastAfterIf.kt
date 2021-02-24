// FIR_COMPARISON
interface Expr
class Num(val value : Int) : Expr

fun eval(e : Expr) {
    if (e is Num) {
        return e.<caret>
    }
}

// EXIST: value




