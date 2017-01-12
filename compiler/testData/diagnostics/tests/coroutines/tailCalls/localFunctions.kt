suspend fun baz() = 1
suspend fun unit() {}

suspend fun foo() {
    <!WRONG_MODIFIER_TARGET!>suspend<!> fun bar() {
        baz()
        return unit()
    }

    <!WRONG_MODIFIER_TARGET!>suspend<!> fun foobar1(): Int {
        return baz()
    }

    <!WRONG_MODIFIER_TARGET!>suspend<!> fun foobar2() {
        return unit()
    }
}
