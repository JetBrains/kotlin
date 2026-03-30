// RUN_PIPELINE_TILL: BACKEND
open class A {
    fun foo() = 42

    object B: A()
}

fun test() {
    (A::foo)(A.B)
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, integerLiteral, nestedClass,
objectDeclaration */
