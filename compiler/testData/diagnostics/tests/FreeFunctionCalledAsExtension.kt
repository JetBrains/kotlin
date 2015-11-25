fun foo(a: (String) -> Unit) {
    "".<!INVOKE_EXTENSION_ON_NOT_EXTENSION_FUNCTION!>a<!>()
}



interface A : (String) -> Unit {}

fun foo(a: @ExtensionFunctionType A) {
    // @ExtensionFunctionType annotation on an unrelated type shouldn't have any effect on this diagnostic.
    // Only kotlin.Function{n} type annotated with @ExtensionFunctionType should
    "".<!INVOKE_EXTENSION_ON_NOT_EXTENSION_FUNCTION!>a<!>()
}
