// RUN_PIPELINE_TILL: FRONTEND

class A {
    annotation class Deprecated<T>(val msg: String)

    // On COMPILER_REQUIRED_ARGUMENTS type resolver returns
    // error type (with "wrong number of type arguments" diagnostic)
    // here. Because of that, result is not stored on that phase
    // and we don't get ambiguity on TYPES.
    @Deprecated<String>("msg")
    fun foo() = Unit

    @<!INAPPLICABLE_CANDIDATE!>Deprecated<!><<!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>Unresolved<!>>("msg")
    fun bar() = Unit
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, nestedClass, nullableType,
primaryConstructor, propertyDeclaration, stringLiteral, typeParameter */
