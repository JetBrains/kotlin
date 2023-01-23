fun foo(a: (String) -> Unit) {
    "".<!UNRESOLVED_REFERENCE!>a<!>()
}



interface A : (String) -> Unit {}
typealias AliasedEFT = ExtensionFunctionType

fun foo(a: <!WRONG_EXTENSION_FUNCTION_TYPE!>@AliasedEFT<!> A) {
    // @Extension annotation on an unrelated type shouldn't have any effect on this diagnostic.
    // Only kotlin.Function{n} type annotated with @Extension should
    "".a()
}
