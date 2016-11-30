suspend fun baz() = 1
suspend fun unit() {}

suspend fun foo() {
    suspend fun bar() {
        <!SUSPENSION_CALL_MUST_BE_USED_AS_RETURN_VALUE, SUSPENSION_CALL_MUST_BE_USED_AS_RETURN_VALUE!>baz()<!>
        return unit()
    }

    suspend fun foobar1(): Int {
        return baz()
    }

    suspend fun foobar2() {
        return unit()
    }
}
