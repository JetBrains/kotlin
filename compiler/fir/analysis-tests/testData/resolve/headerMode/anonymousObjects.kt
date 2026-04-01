// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP

private fun createPrivateObject() =
        object {
            fun foo(): String = "foo"
        }

fun useAnonObject() {
    createAnonObject().foo()
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration, stringLiteral */
