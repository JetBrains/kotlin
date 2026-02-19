// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
interface Tr {
    fun foo()
}

class C(d: <!UNSUPPORTED!>dynamic<!>) : Tr by d

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, inheritanceDelegation, interfaceDeclaration,
primaryConstructor */
