// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
abstract class A {
    abstract val i: Int
}

class B() : A() {
    override val i = 1
}

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, override, primaryConstructor, propertyDeclaration */
