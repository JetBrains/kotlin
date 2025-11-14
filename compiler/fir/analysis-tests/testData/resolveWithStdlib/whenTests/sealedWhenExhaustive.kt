// RUN_PIPELINE_TILL: FRONTEND

sealed class Expr {
    data class Const(val value: Int) : Expr()
    data class Sum(val left: Expr, val right: Expr) : Expr()
    data object Zero : Expr()
}

fun evalSealed(e: Expr): Int =
    when (e) {
        is Expr.Const -> e.value
        is Expr.Sum -> evalSealed(e.left) + evalSealed(e.right)
        Expr.Zero -> 0
    }