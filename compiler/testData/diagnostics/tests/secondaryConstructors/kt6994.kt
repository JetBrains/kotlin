// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
class X<T> {
    constructor(t: T, i: Int): this(<!TYPE_MISMATCH!>i<!>, 1)
}

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, nullableType, secondaryConstructor, typeParameter */
