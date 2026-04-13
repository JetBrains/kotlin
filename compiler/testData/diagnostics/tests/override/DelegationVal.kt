// RUN_PIPELINE_TILL: BACKEND
package delegation

interface Aaa {
    val i: Int
}

class Bbb(aaa: Aaa) : Aaa by aaa

/* GENERATED_FIR_TAGS: classDeclaration, inheritanceDelegation, interfaceDeclaration, primaryConstructor,
propertyDeclaration */
