// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// RETURN_VALUE_CHECKER_MODE: CHECKER

@MustUseReturnValue
interface Ia {
    fun foo(): Any // Implicitly @MustUse
}

interface Ib {
    fun foo(): String // Implicitly @Ignorable
}

interface I: Ia, Ib

fun usage(i: I) {
    i.foo() // (1) always resolved to Ib.foo(): String
}

fun <T> usage2(i: T) where T: Ia, T: Ib {
    i.foo()
}

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration */
