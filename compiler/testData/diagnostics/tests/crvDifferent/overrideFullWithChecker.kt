// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// MODULE: lib0
// RETURN_VALUE_CHECKER_MODE: FULL

// FILE: Base.kt

interface Base {
    fun foo(): String
}

// MODULE: lib1(lib0)
// RETURN_VALUE_CHECKER_MODE: CHECKER

// FILE: Lib.kt

interface I1: Base {
    override fun foo(): String
}

@MustUseReturnValue
interface I2: Base {
    override fun foo(): String
}

@MustUseReturnValue
interface I3: Base {
    @IgnorableReturnValue override fun foo(): String
}

fun checkLib(i1: I1, i2: I2, i3: I3) {
    i1.foo()
    i2.foo()
    i3.foo()
}

// MODULE: main(lib0, lib1)
// RETURN_VALUE_CHECKER_MODE: FULL

// FILE: App.kt

class C1: I1 {
    override fun foo(): String = ""
}

class C2: I2 {
    override fun foo(): String = ""
}

open class C3: I3 {
    override fun foo(): String = ""
}

@MustUseReturnValue
class C4: I3 {
    override fun foo(): String = "" // Should be a warning on overriding explicit @Ignorable
}

@MustUseReturnValue
class C5: C3() {
    override fun foo(): String = ""
}

fun check(c1: C1, c2: C2, c3: C3, c4: C4, c5: C5) {
    c1.foo()
    c2.foo()
    c3.foo()
    c4.foo()
    c5.foo()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, override */
