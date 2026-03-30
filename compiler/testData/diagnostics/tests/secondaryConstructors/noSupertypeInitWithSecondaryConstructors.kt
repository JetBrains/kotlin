// RUN_PIPELINE_TILL: BACKEND
open class B
interface C
interface D
class A : C, B, D {
    constructor()
}

/* GENERATED_FIR_TAGS: classDeclaration, interfaceDeclaration, secondaryConstructor */
