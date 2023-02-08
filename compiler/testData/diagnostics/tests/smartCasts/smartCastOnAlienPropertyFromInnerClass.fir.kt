// MODULE: m1
// FILE: A.kt

open class Base(val x: Any)

// MODULE: m2(m1)
// FILE: B.kt

private class Derived : Base("123") {
    fun foo() {
        if (x is String) {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.length
        }
    }
}
