// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-77451, KT-17417

object B : A by B {
    override val bar = ""
}

interface A {
    val bar: String
}

/* GENERATED_FIR_TAGS: inheritanceDelegation, interfaceDeclaration, objectDeclaration, override, propertyDeclaration,
stringLiteral */
