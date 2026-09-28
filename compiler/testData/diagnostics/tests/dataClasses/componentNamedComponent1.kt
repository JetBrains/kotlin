// RUN_PIPELINE_TILL: CODEGEN
data class A(val component1: Int)

fun foo(a: A) {
    a.component1()
    a.component1
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, primaryConstructor, propertyDeclaration */
