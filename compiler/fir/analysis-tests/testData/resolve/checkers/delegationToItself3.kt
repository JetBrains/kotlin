// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ForbidObjectDelegationToItself
// ISSUE: KT-17417

interface A {
    fun foo() = 0
}

<!ABSTRACT_MEMBER_INCORRECTLY_DELEGATED_WARNING!>object B<!> : A by B

/* GENERATED_FIR_TAGS: functionDeclaration, inheritanceDelegation, integerLiteral, interfaceDeclaration,
objectDeclaration */
