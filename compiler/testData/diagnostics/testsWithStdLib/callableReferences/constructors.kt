// RUN_PIPELINE_TILL: CODEGEN
class Klass {
    constructor(a: Int) {}
    constructor(a: String) {}
}

fun user(f: (Int) -> Klass) {}

fun fn() {
    user(::Klass)
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, functionalType, secondaryConstructor */
