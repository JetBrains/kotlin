// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_ANONYMOUS_PARAMETER
package a

interface Super
interface Trait : Super
class Sub : Trait

fun foo(f: (Trait) -> Trait) = f

fun test(s: Sub) {
    foo {
        t: Super -> s
    }
    foo {
        t: Trait -> s
    }

    foo(fun(t: Sub) = <!ARGUMENT_TYPE_MISMATCH!>s<!>)
    foo(fun(t): Super = <!ARGUMENT_TYPE_MISMATCH!>s<!>)
}
