// MODULE[jvm]: m1
// FILE: k.kt

val foo: <!UNSUPPORTED!>dynamic<!> = 1

fun foo() {
    class C {
        val foo: <!UNSUPPORTED!>dynamic<!> = 1
    }
}

// MODULE[js]: m2
// FILE: k.kt

val foo: dynamic = 1

fun foo() {
    class C {
        val foo: dynamic = 1
    }
}