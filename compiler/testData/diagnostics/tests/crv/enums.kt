// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB


@MustUseReturnValues
enum class WithMRV {
    A, B;
    fun foo() = ""
}

enum class WithoutMRV {
    A, B;
    fun foo() = ""
}

fun main() {
    WithMRV.A
    WithMRV.A.foo()
    WithoutMRV.A // Should we ALWAYS report enum entries?
    WithoutMRV.A.foo()
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, stringLiteral */
