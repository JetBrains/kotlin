// RUN_PIPELINE_TILL: BACKEND
//KT-591 Unresolved label in valid code

fun test() {
    val a: (Int?).() -> Unit = a@{
        if (this != null) {
            val b: String.() -> Unit = {
                this@a.times(5) // a@ Unresolved
            }
        }
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, functionalType, ifExpression, integerLiteral,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, smartcast, thisExpression, typeWithExtension */
