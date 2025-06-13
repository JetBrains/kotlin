// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
class X<T>(val t: T) {
    constructor(t: T, i: Int) : this(<!TYPE_MISMATCH!>i<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, primaryConstructor, propertyDeclaration, secondaryConstructor,
typeParameter */
