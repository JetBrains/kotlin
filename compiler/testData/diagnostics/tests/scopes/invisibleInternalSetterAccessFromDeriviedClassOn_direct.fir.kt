// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE:+ProhibitAccessToInvisibleSetterFromDerivedClass

// MODULE: m1
// FILE: base.kt

open class Base {
    var foo: String = ""
        internal set
}

// MODULE: m2(m1)
// FILE: derived1.kt

fun testBase(b: Base) {
    b.<!INVISIBLE_SETTER!>foo<!> = "other"
}

open class Derived1(foo: String) : Base() {
    init {
        this.<!INVISIBLE_SETTER!>foo<!> = foo
    }

    fun bar(param: String) {
        <!INVISIBLE_SETTER!>foo<!> = param
        this.<!INVISIBLE_SETTER!>foo<!> = param
    }
}

open class Derived2 : Derived1("")

fun testFunction(d: Derived1) {
    d.<!INVISIBLE_SETTER!>foo<!> = "other"
}
