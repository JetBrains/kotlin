// RUN_PIPELINE_TILL: CODEGEN
fun Runnable(f: () -> Unit): Runnable = object : Runnable {
    public override fun run() {
        f()
    }
}

val x = Runnable {  }

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration, functionalType, lambdaLiteral, override,
propertyDeclaration */
