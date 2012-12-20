package a

trait Super
trait Trait : Super
class Sub : Trait

fun foo(f: (Trait) -> Trait) = f

fun test(s: Sub) {
    foo {
        (<!EXPECTED_PARAMETER_TYPE_MISMATCH!>t: Super<!>): <!EXPECTED_RETURN_TYPE_MISMATCH!>Sub<!> -> s
    }
    foo {
        (t: Trait): Trait -> s
    }
    foo {
        (t: Sub): Super -> s
    }
}
