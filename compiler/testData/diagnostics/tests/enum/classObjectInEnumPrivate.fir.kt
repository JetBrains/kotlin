enum class E {
    ENTRY;

    private companion object
}

fun foo() = E.values()
fun bar() = E.valueOf("ENTRY")
fun baz() = E.ENTRY
<!EXPOSED_FUNCTION_RETURN_TYPE!>fun quux() = E<!>