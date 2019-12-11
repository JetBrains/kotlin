// !WITH_NEW_INFERENCE
// !CHECK_TYPE

interface A
interface B : A
interface C : B

fun test(b: B) {
    b checkType { <!UNRESOLVED_REFERENCE!>_<!><B>() }
    b checkType { <!UNRESOLVED_REFERENCE!>_<!><A>() }
    b checkType { <!UNRESOLVED_REFERENCE!>_<!><C>() }
}
