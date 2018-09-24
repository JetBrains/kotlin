// !WITH_NEW_INFERENCE
// !CHECK_TYPE
val x get() = null
val <!IMPLICIT_NOTHING_PROPERTY_TYPE!>y<!> get() = null!!

fun foo() {
    <!DEBUG_INFO_CONSTANT!>x<!> checkType { _<Nothing?>() }
    y <!UNREACHABLE_CODE!>checkType { <!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Nothing>() }<!>
}
