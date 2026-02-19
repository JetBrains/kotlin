// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
interface T {
    fun component1(): Int
}

data class A(val x: Int) : T

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, interfaceDeclaration, primaryConstructor,
propertyDeclaration */
