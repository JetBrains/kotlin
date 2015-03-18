package a

trait Super
trait Trait : Super
class Sub : Trait

fun foo(f: (Trait) -> Trait) = f

fun test(s: Sub) {
    foo {
        <!DEPRECATED_LAMBDA_SYNTAX!>(t: Super): Sub<!> -> s
    }
    foo {
        <!DEPRECATED_LAMBDA_SYNTAX!>(t: Trait): Trait<!> -> s
    }
    foo {
        <!DEPRECATED_LAMBDA_SYNTAX!>(<!EXPECTED_PARAMETER_TYPE_MISMATCH!>t: Sub<!>): <!EXPECTED_RETURN_TYPE_MISMATCH!>Super<!><!> -> s
    }
}
