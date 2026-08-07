// RUN_PIPELINE_TILL: FRONTEND

// MODULE: lib

// FILE: Lib.kt

interface Base {
    override fun equals(@EqualityBound(Base::class) other: Any?): Boolean
}

// MODULE: main(lib)

// FILE: main.kt

interface Other

class Bad : Base, Other {
    <!EQUALITY_BOUND_MISMATCH_ON_INHERITANCE!>override fun equals(@EqualityBound(Other::class) other: Any?): Boolean = true<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, interfaceDeclaration, nullableType,
operator, override */
