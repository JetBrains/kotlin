// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE:-ProhibitAccessToInvisibleSetterFromDerivedClass

// MODULE: m1
// FILE: base.kt

open class Base {
    var foo: String = ""
        internal set
}

// MODULE: m2(m1)
// FILE: derived1.kt

open class Derived1(foo: String) : Base()
open class Derived2 : Derived1("")

// MODULE: m3(m2, m1)
// FILE: derived2.kt

fun testDerivied1(d: Derived1) {
    <!INVISIBLE_SETTER!>d.foo<!> = "other"
}

fun testDerivied2(d: Derived2) {
    <!INVISIBLE_SETTER!>d.foo<!> = "other"
}

class Derivied3(foo: String) : Derived2() {
    init {
        <!INVISIBLE_SETTER_FROM_DERIVED!>this.foo<!> = foo
    }

    fun bar1(param: String) {
        <!INVISIBLE_SETTER_FROM_DERIVED!>foo<!> = param
        <!INVISIBLE_SETTER_FROM_DERIVED!>this.foo<!> = param
    }
}
