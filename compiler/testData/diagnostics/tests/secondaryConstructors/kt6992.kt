// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
class X<T>(val t: T) {
    constructor(t: String): <!CYCLIC_CONSTRUCTOR_DELEGATION_CALL!>this<!>(t)
}

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, primaryConstructor, propertyDeclaration, secondaryConstructor,
typeParameter */
