// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidObjectDelegationToItself
// ISSUE: KT-17417

interface A {
    fun foo(): Int

    val bar: String
}

<!ABSTRACT_MEMBER_INCORRECTLY_DELEGATED_ERROR!>object B<!> : A by B

/* GENERATED_FIR_TAGS: functionDeclaration, inheritanceDelegation, interfaceDeclaration, objectDeclaration,
propertyDeclaration */
