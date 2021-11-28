interface Expr
class BinOp(val operator : String) : Expr

fun test(e : Expr) {
    if (e is BinOp) {
        when (<!DEBUG_INFO_SMARTCAST!>e<!>.operator) {
            else -> 0
        }
    }
}
