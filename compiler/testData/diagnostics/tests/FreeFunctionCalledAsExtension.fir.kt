fun foo(a: (String) -> Unit) {
    "".<!UNRESOLVED_REFERENCE!>a<!>()
}



interface A : (String) -> Unit {}

fun foo(a: @ExtensionFunctionType A) {
    // @Extension annotation on an unrelated type shouldn't have any effect on this diagnostic.
    // Only kotlin.Function{n} type annotated with @Extension should
    "".a()
}
