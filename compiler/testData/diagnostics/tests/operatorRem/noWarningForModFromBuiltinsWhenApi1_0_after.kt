// !LANGUAGE: +ProhibitOperatorMod
// !WITH_NEW_INFERENCE
// !API_VERSION: 1.0
// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE, -EXTENSION_SHADOWED_BY_MEMBER

class Foo {
    operator fun rem(x: Int): Foo = Foo()
}

class Bar {
    operator fun remAssign(x: Int) {}
}

class Baz {
    companion object {
        operator fun rem(x: Int) {}
        operator fun Int.rem(x: Int) {}
    }
}

operator fun Baz.rem(x: Int) {}

fun local() {
    operator fun Int.rem(x: Int) {}
    operator fun String.remAssign(x: Int) {}
}

class WithMod {
    <!FORBIDDEN_BINARY_MOD!>operator<!> fun mod(other: WithMod) = this

    fun test() {
        val a = this <!OI;FORBIDDEN_BINARY_MOD_AS_REM!>%<!> <!NI;TYPE_MISMATCH!>this<!>
        var b = this.mod(this)
        <!NI;TYPE_MISMATCH!>b <!OI;FORBIDDEN_BINARY_MOD_AS_REM!>%=<!> <!NI;TYPE_MISMATCH!>this<!><!>
    }
}

fun noOverflow() {
    (-1).mod(5)
}

fun builtIns(b: Byte, s: Short) {
    var a = 1 % 2
    a <!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE!>%=<!> 3
    1.mod(2)
    b % <!NI;TYPE_MISMATCH!>s<!>
    1.0 % <!NI;CONSTANT_EXPECTED_TYPE_MISMATCH!>2.0<!>
}