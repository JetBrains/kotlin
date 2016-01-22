fun foo(<!UNUSED_PARAMETER!>a<!>: (String) -> Unit) {
    "".<!UNRESOLVED_REFERENCE!>a<!>()
}



interface A : (String) -> Unit {}

fun foo(<!UNUSED_PARAMETER!>a<!>: @ExtensionFunctionType A) {
    // @Extension annotation on an unrelated type shouldn't have any effect on this diagnostic.
    // Only kotlin.Function{n} type annotated with @Extension should
    "".<!UNRESOLVED_REFERENCE!>a<!>()
}
