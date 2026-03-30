// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-68724

interface A {
    fun foo()
}

<!REDUNDANT_MODIFIER_FOR_TARGET!>open<!> interface B : A {
}

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration */
