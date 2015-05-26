fun foo(a: (String) -> Unit) {
    "".<!FREE_FUNCTION_CALLED_AS_EXTENSION!>a<!>()
}



interface A : (String) -> Unit {}

fun foo(a: @extension A) {
    // @extension annotation on an unrelated type shouldn't have any effect on this diagnostic.
    // Only kotlin.Function{n} type annotated with @extension should
    "".<!FREE_FUNCTION_CALLED_AS_EXTENSION!>a<!>()
}
