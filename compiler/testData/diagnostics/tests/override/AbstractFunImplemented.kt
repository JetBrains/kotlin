// RUN_PIPELINE_TILL: BACKEND
abstract class A {
    abstract fun foo(): Int
}

class B() : A() {
    override fun foo() = 1
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, override, primaryConstructor */
