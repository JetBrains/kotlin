// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// SKIP_TXT
// FILE: main.kt

open class A {
    protected open val x: (String) -> Boolean = { true }
}

class B : A() {
    override val x = { y: String ->
        super.x(y)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionalType, lambdaLiteral, override, propertyDeclaration, superExpression */
