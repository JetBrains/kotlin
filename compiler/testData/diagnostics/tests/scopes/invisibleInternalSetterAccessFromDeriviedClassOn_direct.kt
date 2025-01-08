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
    <!INVISIBLE_SETTER!>b.foo<!> = "other"
}

open class Derived1(foo: String) : Base() {
    init {
        <!INVISIBLE_SETTER!>this.<!DEBUG_INFO_LEAKING_THIS!>foo<!><!> = foo
    }

    fun bar(param: String) {
        <!INVISIBLE_SETTER!>foo<!> = param
        <!INVISIBLE_SETTER!>this.foo<!> = param
    }
}

open class Derived2 : Derived1("")

fun testFunction(d: Derived1) {
    <!INVISIBLE_SETTER!>d.foo<!> = "other"
}
