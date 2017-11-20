// !WITH_NEW_INFERENCE
// !CHECK_TYPE

interface A
interface B : A
interface C : B

fun test(b: B) {
    b checkType { _<B>() }
    b checkType { <!TYPE_MISMATCH!>_<!><A>() }
    b checkType { <!TYPE_MISMATCH!>_<!><C>() }
}
