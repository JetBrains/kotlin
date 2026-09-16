// RUN_PIPELINE_TILL: CODEGEN
package delegation

interface Aaa {
    fun foo()
}

class Bbb(aaa: Aaa) : Aaa by aaa

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inheritanceDelegation, interfaceDeclaration,
primaryConstructor */
