// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// SKIP_TXT

val w: Int = 2

class Outer {
    private inner class Inner private constructor(x: Int) {
        constructor() : this(w)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, inner, integerLiteral, primaryConstructor, propertyDeclaration,
secondaryConstructor */
