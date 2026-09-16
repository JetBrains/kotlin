// RUN_PIPELINE_TILL: CODEGEN
fun interface Test {
    fun foo()
}

val f = ::Test

/* GENERATED_FIR_TAGS: callableReference, funInterface, functionDeclaration, interfaceDeclaration, propertyDeclaration */
