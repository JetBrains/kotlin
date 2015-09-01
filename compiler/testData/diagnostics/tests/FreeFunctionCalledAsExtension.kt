fun foo(a: (String) -> Unit) {
    "".<!FREE_FUNCTION_CALLED_AS_EXTENSION!>a<!>()
}



interface A : (String) -> Unit {}

fun foo(a: @Extension A) {
    // @Extension annotation on an unrelated type shouldn't have any effect on this diagnostic.
    // Only kotlin.Function{n} type annotated with @Extension should
    "".<!FREE_FUNCTION_CALLED_AS_EXTENSION!>a<!>()
}
