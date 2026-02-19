// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
class A<T1, T2> {
    constructor(block: (T1) -> T2)
    constructor(x: T2): this({ x })
}

/* GENERATED_FIR_TAGS: classDeclaration, functionalType, lambdaLiteral, nullableType, secondaryConstructor,
typeParameter */
