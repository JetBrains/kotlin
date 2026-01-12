// RUN_PIPELINE_TILL: FRONTEND
sealed class Stmt

class ForStmt : Stmt()

sealed class Expr : Stmt() {
    object BinExpr : Expr()
}

fun test(x: Stmt): String =
        <!WHEN_ON_SEALED!>when (x) {
            is Expr -> "expr"
            is Stmt -> "stmt"
        }<!>

fun test2(x: Stmt): String =
        <!NO_ELSE_IN_WHEN!>when<!> (x) {
            is Expr -> "expr"
        }

fun test3(x: Expr): String =
        <!WHEN_ON_SEALED!>when (x) {
            <!USELESS_IS_CHECK!>is Stmt<!> -> "stmt"
        }<!>

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, isExpression, nestedClass, objectDeclaration, sealed,
smartcast, stringLiteral, whenExpression, whenWithSubject */
