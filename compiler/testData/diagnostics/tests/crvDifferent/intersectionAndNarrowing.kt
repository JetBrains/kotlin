// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// RETURN_VALUE_CHECKER_MODE: CHECKER

@MustUseReturnValue
interface Ia {
    fun foo(): Any // Implicitly @MustUse
}

@MustUseReturnValue
interface Ib {
    fun foo(): String // Implicitly @Ignorable or @MustUse
}

interface Ib_ign {
    fun foo(): String
}

interface Ic {
    fun foo(): String // Implicitly @Ignorable
}

interface I1: Ia, Ib, Ic // (2)

interface I2: Ia, Ic, Ib

interface I3: Ia, Ic, Ib_ign


fun usage(i1: I1, i2: I2, i3: I3) {
    i1.foo() // (3)
    i2.foo()
    i3.foo()
}

fun <T> usage2(i: T) where T: Ib_ign, T: Ia, T: Ic {
    i.foo()
}

fun <T> usage3(i: T) where T: Ia, T: Ib_ign, T: Ic {
    i.foo()
}

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration */
