// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class A {
    enum class E {
        E1,
        E2 { };
    }
}

fun foo() = A.E.E1
fun bar() = A.E.E2

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, functionDeclaration, nestedClass */
