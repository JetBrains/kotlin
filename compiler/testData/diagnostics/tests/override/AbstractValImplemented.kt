// RUN_PIPELINE_TILL: BACKEND
abstract class A {
    abstract val i: Int
}

class B() : A() {
    override val i = 1
}

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, override, primaryConstructor, propertyDeclaration */
