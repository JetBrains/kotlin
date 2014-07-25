trait Expr
class BinOp(val operator : String) : Expr

fun test(e : Expr) {
    if (e is BinOp) {
        when (<!DEBUG_INFO_AUTOCAST!>e<!>.operator) {
            else -> <!UNUSED_EXPRESSION!>0<!>
        }
    }
}