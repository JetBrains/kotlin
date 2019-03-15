// !LANGUAGE: -ProhibitOperatorMod
// !DIAGNOSTICS: -UNUSED_PARAMETER

class A {
    companion object {
        operator fun A.rem(x: Int) = 0
    }

    fun test() {
        <!DEPRECATED_BINARY_MOD!>operator<!> fun A.mod(x: Int) = ""

        takeInt(A() % 123)
    }
}

fun takeInt(x: Int) {}