// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

sealed class Expr
data class Num(val n: Int): Expr()
object End: Expr()

fun eval(e: Expr): Int =
<!NO_ELSE_IN_WHEN!>when<!>(e) {
    is Num -> e.n
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, isExpression, objectDeclaration, primaryConstructor,
propertyDeclaration, sealed, smartcast, whenExpression, whenWithSubject */
