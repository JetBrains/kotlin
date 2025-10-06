// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT

interface Base<T> {
    fun foo(arg: Base<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><T, String><!>)

    fun bar(arg: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Base<!>)

    fun bar(arg: Base<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><T, T><!>)
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class Derived<!><S> : Base<S> {
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo(arg: Base<S>) {}

    <!NOTHING_TO_OVERRIDE!>override<!> fun bar(arg: Base<S>) {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, nullableType, override,
typeParameter */
