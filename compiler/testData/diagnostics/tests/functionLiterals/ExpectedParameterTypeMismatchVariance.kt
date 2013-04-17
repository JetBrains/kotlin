package a

trait Super
trait Trait : Super
class Sub : Trait

fun foo(f: (Trait) -> Trait) = f

fun test(s: Sub) {
    foo {
        (t: Super): Sub -> s
    }
    foo {
        (t: Trait): Trait -> s
    }
    foo {
        (<!EXPECTED_PARAMETER_TYPE_MISMATCH!>t: Sub<!>): <!EXPECTED_RETURN_TYPE_MISMATCH!>Super<!> -> s
    }
}
