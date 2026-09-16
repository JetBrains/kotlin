// RUN_PIPELINE_TILL: CODEGEN
abstract class A {
    abstract var i: Int
}

class B() : A() {
    override var i = 1
}

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, override, primaryConstructor, propertyDeclaration */
