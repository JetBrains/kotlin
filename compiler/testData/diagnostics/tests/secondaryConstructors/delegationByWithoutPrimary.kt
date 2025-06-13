// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
interface A
class AImpl : A

class B : <!UNSUPPORTED!>A by AImpl()<!> {
    constructor()
}

/* GENERATED_FIR_TAGS: classDeclaration, inheritanceDelegation, interfaceDeclaration, secondaryConstructor */
