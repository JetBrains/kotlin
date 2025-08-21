// RUN_PIPELINE_TILL: BACKEND
interface Expr
class BinOp(val operator : String) : Expr

fun test(e : Expr) {
    if (e is BinOp) {
        when (<!DEBUG_INFO_SMARTCAST!>e<!>.operator) {
            else -> 0
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, integerLiteral, interfaceDeclaration,
isExpression, primaryConstructor, propertyDeclaration, whenExpression, whenWithSubject */
