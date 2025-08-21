// RUN_PIPELINE_TILL: BACKEND
val f: (String.() -> String)? = null

fun box(): String {
    val g = when {
        f != null -> <!DEBUG_INFO_SMARTCAST!>f<!>
        else -> {
            { this + "K" }
        }
    }
    return g("O")
}

/* GENERATED_FIR_TAGS: additiveExpression, equalityExpression, functionDeclaration, functionalType, lambdaLiteral,
localProperty, nullableType, propertyDeclaration, smartcast, stringLiteral, thisExpression, typeWithExtension,
whenExpression */
