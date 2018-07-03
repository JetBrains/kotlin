// !LANGUAGE: -ProhibitOperatorMod
// !DIAGNOSTICS: -UNUSED_PARAMETER

class A {
    <!DEPRECATED_BINARY_MOD!>operator<!> fun Int.mod(s: String) = 4
}

class B {
    operator fun Int.rem(s: String) = ""
}

fun test() {
    with(B()) {
        with(A()) {
            takeString(1 % "")
        }
    }
}

fun takeString(s: String) {}