enum class E {
    ENTRY;

    private companion object
}

fun foo() = E.values()
fun bar() = E.valueOf("ENTRY")
fun baz() = E.ENTRY
fun <!EXPOSED_FUNCTION_RETURN_TYPE!>quux<!>() = <!INVISIBLE_REFERENCE!>E<!>
