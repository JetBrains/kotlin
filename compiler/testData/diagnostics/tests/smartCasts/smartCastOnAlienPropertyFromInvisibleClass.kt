// !LANGUAGE: -ProhibitSmartcastsOnPropertyFromAlienBaseClassInheritedInInvisibleClass
// RENDER_DIAGNOSTICS_FULL_TEXT
// MODULE: m1
// FILE: A.kt

open class Base(val x: Any)

// MODULE: m2(m1)
// FILE: B.kt

private class Derived : Base("123") {
    fun foo() {
        if (x is String) {
            <!DEBUG_INFO_SMARTCAST, DEPRECATED_SMARTCAST!>x<!>.length
        }
    }
}

internal class Internal : Base("456")

internal fun bar(i: Internal) {
    if (i.x is String) {
        <!DEBUG_INFO_SMARTCAST, DEPRECATED_SMARTCAST!>i.x<!>.length
    }
}
