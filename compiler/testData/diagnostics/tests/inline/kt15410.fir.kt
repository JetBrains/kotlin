// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

open class Foo protected constructor()

inline fun foo(f: () -> Unit) = object: Foo() {}

class A : Foo() {
    inline fun foo(f: () -> Unit) = <!PROTECTED_CONSTRUCTOR_CALL_FROM_PUBLIC_INLINE!>Foo<!>()
}
