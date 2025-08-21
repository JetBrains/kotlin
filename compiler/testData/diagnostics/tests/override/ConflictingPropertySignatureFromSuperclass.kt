// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER -INCORRECT_TYPE_PARAMETER_OF_PROPERTY
open class Aaa() {
    val bar = 1
}

open class Bbb() : Aaa() {
    <!CONFLICTING_OVERLOADS!>val <T> bar<!> = "aa"
}

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, nullableType, primaryConstructor, propertyDeclaration,
stringLiteral, typeParameter */
