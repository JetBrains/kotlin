fun foo(a: (String) -> Unit) {
    "".<!INVOKE_EXTENSION_ON_NOT_EXTENSION_FUNCTION!>a<!>()
}



interface A : (String) -> Unit {}

fun foo(a: @Extension A) {
    // @Extension annotation on an unrelated type shouldn't have any effect on this diagnostic.
    // Only kotlin.Function{n} type annotated with @Extension should
    "".<!INVOKE_EXTENSION_ON_NOT_EXTENSION_FUNCTION!>a<!>()
}