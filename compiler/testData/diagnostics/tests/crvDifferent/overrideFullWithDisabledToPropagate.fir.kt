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

@MustUseReturnValue
class C3: C1() {
    override fun foo(): String = ""
    override fun <!OVERRIDING_IGNORABLE_WITH_MUST_USE!>bar<!>(): String = ""
}

fun testFoo(d1: D1, d2: D2, d3: D3, d4: D4, c1: C1, c2: C2, c3: C3) {
    d1.<!RETURN_VALUE_NOT_USED!>foo<!>()
    d2.<!RETURN_VALUE_NOT_USED!>foo<!>()
    d3.<!RETURN_VALUE_NOT_USED!>foo<!>()
    d4.<!RETURN_VALUE_NOT_USED!>foo<!>()
    c1.<!RETURN_VALUE_NOT_USED!>foo<!>()
    c2.<!RETURN_VALUE_NOT_USED!>foo<!>()
    c3.<!RETURN_VALUE_NOT_USED!>foo<!>()
}

fun testBar(d1: D1, d2: D2, d3: D3, d4: D4, c1: C1, c2: C2, c3: C3) {
    d1.bar()
    d2.bar()
    d3.bar()
    d4.bar()
    c1.bar()
    c2.bar()
    c3.<!RETURN_VALUE_NOT_USED!>bar<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inheritanceDelegation, interfaceDeclaration, override,
primaryConstructor, propertyDeclaration, stringLiteral */
