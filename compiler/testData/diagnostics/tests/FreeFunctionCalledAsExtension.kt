fun foo(a: (String) -> Unit) {
    "".<!FREE_FUNCTION_CALLED_AS_EXTENSION!>a<!>()
}
