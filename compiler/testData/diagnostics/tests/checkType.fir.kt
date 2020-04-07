// !WITH_NEW_INFERENCE
// !CHECK_TYPE

interface A
interface B : A
interface C : B

fun test(b: B) {
    b checkType { _<B>() }
    b checkType { <!INAPPLICABLE_CANDIDATE!>_<!><A>() }
    b checkType { <!INAPPLICABLE_CANDIDATE!>_<!><C>() }
}
