// RUN_PIPELINE_TILL: BACKEND

fun local() {
    val x: Int
    object {
        val foo: Any field: Int

        init {
            x = 0
            foo = x
        }
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, assignment, explicitBackingField, functionDeclaration, init,
integerLiteral, localProperty, propertyDeclaration */
