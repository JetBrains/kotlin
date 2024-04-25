// LANGUAGE: -ProhibitSmartcastsOnPropertyFromAlienBaseClass
// MODULE: m1
// FILE: A.kt

open class Base(val x: Any)
open class Generic<T>(val y: T)

// MODULE: m2(m1)
// FILE: B.kt

class Derived : Base("123") {
    fun foo() {
        if (x is String) {
            <!DEBUG_INFO_SMARTCAST, DEPRECATED_SMARTCAST!>x<!>.length // impossible since `x` is in another module. FE1.0 allows this due to KT-47225
        }
    }
}

class MyGeneric : Generic<Number>(42) {
    private fun baz(arg: Int) {}
    fun bar() {
        if (y is Int) {
            baz(<!DEBUG_INFO_SMARTCAST, DEPRECATED_SMARTCAST!>y<!>) // impossible since `y` is in another module. FE1.0 allows this due to KT-47225
        }
    }
}
