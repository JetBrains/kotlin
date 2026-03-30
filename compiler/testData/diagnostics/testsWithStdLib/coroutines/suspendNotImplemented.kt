// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-63233, KT-59818

interface A {
    suspend fun foo()
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class B<!> : A

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, suspend */
