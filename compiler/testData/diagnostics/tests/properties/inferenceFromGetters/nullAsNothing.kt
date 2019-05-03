// !WITH_NEW_INFERENCE
// !CHECK_TYPE
val x get() = null
val <!IMPLICIT_NOTHING_PROPERTY_TYPE!>y<!> get() = null!!

fun foo() {
    <!DEBUG_INFO_CONSTANT!>x<!> checkType { _<Nothing?>() }
    y <!UNREACHABLE_CODE!><!IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>checkType<!> { _<Nothing>() }<!>
}
