// RUN_PIPELINE_TILL: FRONTEND

class A<T> {
    override fun equals(@EqualityBound(<!CLASS_LITERAL_LHS_NOT_A_CLASS!>A<*>::class<!>) other: Any?): Boolean = true
}

class B<T> {
    override fun equals(@EqualityBound(<!CLASS_LITERAL_LHS_NOT_A_CLASS!><!AMBIGUOUSLY_RESOLVED_EQUALITY_BOUND_ARGUMENT!>B<String><!>::class<!>) other: Any?): Boolean = true
}

class C<T> {
    override fun equals(@EqualityBound(<!CLASS_LITERAL_LHS_NOT_A_CLASS!><!AMBIGUOUSLY_RESOLVED_EQUALITY_BOUND_ARGUMENT!>C<T><!>::class<!>) other: Any?): Boolean = true
}

class D<T> {
    override fun equals(@EqualityBound(<!CLASS_LITERAL_LHS_NOT_A_CLASS!><!EQUALITY_BOUND_NOT_SUPERTYPE_OF_CONTAINING_CLASS!>Array<*><!>::class<!>) other: Any?): Boolean = true
}

class E<T> {
    override fun equals(@EqualityBound(<!AMBIGUOUSLY_RESOLVED_EQUALITY_BOUND_ARGUMENT!>Array<String><!>::class) other: Any?): Boolean = true
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, nullableType, operator, override,
typeParameter */
