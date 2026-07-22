// RUN_PIPELINE_TILL: FRONTEND

class A {
    override fun equals(@EqualityBound(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>Unresolved<!>::class<!>) other: Any?): Boolean = true
}

class B {
    override fun equals(@EqualityBound(<!ARGUMENT_TYPE_MISMATCH!>42<!>) other: Any?): Boolean = true
}

class C {
    override fun equals(@EqualityBound(<!ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL!>42::class<!>) other: Any?): Boolean = true
}

class D {
    override fun equals(@EqualityBound(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>D.<!UNRESOLVED_REFERENCE!>Unresolved<!>::class<!>) other: Any?): Boolean = true
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, integerLiteral, nullableType, operator,
override */
