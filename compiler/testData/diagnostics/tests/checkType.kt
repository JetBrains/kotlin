// !WITH_NEW_INFERENCE
// !CHECK_TYPE

interface A
interface B : A
interface C : B

fun test(b: B) {
    b checkType { _<B>() }
    b checkType { <!TYPE_MISMATCH{OI}, UNRESOLVED_REFERENCE_WRONG_RECEIVER{NI}!>_<!><A>() }
    b checkType { <!TYPE_MISMATCH{OI}, UNRESOLVED_REFERENCE_WRONG_RECEIVER{NI}!>_<!><C>() }
}
