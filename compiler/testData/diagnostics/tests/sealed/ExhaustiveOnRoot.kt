sealed class Stmt

class ForStmt : Stmt()

sealed class Expr : Stmt() {
    object BinExpr : Expr()
}

fun test(x: Stmt): String =
        when (x) {
            is Expr -> "expr"
            is Stmt -> "stmt"
        }

fun test2(x: Stmt): String =
        <!NO_ELSE_IN_WHEN!>when<!> (x) {
            is Expr -> "expr"
        }

fun test3(x: Expr): String =
        when (x) {
            is Stmt -> "stmt"
        }