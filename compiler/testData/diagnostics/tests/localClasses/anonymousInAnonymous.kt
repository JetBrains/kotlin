// RUN_PIPELINE_TILL: CODEGEN
fun foo() {
    val base = object {
        fun bar() = object {
            fun buz() = foobar
        }
        val foobar = ""
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration, localProperty, propertyDeclaration, stringLiteral */
