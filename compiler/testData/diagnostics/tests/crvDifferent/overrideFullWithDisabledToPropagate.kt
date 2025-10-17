// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// MODULE: lib0
// RETURN_VALUE_CHECKER_MODE: FULL

// FILE: Base.kt

interface Base {
    fun foo(): String
    @IgnorableReturnValue fun bar(): String
}

// MODULE: lib1(lib0)
// RETURN_VALUE_CHECKER_MODE: DISABLED

// FILE: Lib.kt

open class D1(val b: Base): Base by b

open class D2(val b: Base): Base by b {
    override fun foo(): String = ""
    override fun bar(): String = ""
}

open class C1: Base {
    override fun foo(): String = ""
    override fun bar(): String = ""
}

// MODULE: main(lib0, lib1)
// RETURN_VALUE_CHECKER_MODE: FULL

// FILE: App.kt

class D3(b: Base): D1(b) {
    override fun foo(): String = ""
    override fun bar(): String = ""
}

class D4(b: Base): D2(b) {
    override fun foo(): String = ""
    override fun bar(): String = ""
}

class C2: C1() {
    override fun foo(): String = ""
    override fun bar(): String = ""
}

@MustUseReturnValues
class C3: C1() {
    override fun foo(): String = ""
    override fun bar(): String = ""
}

fun testFoo(d1: D1, d2: D2, d3: D3, d4: D4, c1: C1, c2: C2, c3: C3) {
    d1.foo()
    d2.foo()
    d3.foo()
    d4.foo()
    c1.foo()
    c2.foo()
    c3.foo()
}

fun testBar(d1: D1, d2: D2, d3: D3, d4: D4, c1: C1, c2: C2, c3: C3) {
    d1.bar()
    d2.bar()
    d3.bar()
    d4.bar()
    c1.bar()
    c2.bar()
    c3.bar()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inheritanceDelegation, interfaceDeclaration, override,
primaryConstructor, propertyDeclaration, stringLiteral */
