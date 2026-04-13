// RUN_PIPELINE_TILL: BACKEND
object X

class Y {
    fun f(op: X.() -> Unit) {
        X.op()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, objectDeclaration, typeWithExtension */
