// RUN_PIPELINE_TILL: FRONTEND
val Int.plusAssign: (Int) -> Unit
    get() = {}

fun main() {
    1 <!NOT_FUNCTION_AS_OPERATOR!>+=<!> 2
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, getter, integerLiteral, lambdaLiteral, propertyDeclaration,
propertyWithExtensionReceiver */
