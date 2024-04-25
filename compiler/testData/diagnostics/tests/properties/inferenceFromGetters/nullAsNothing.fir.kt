// CHECK_TYPE
val x get() = null
val <!IMPLICIT_NOTHING_PROPERTY_TYPE!>y<!> get() = null!!

fun foo() {
    x checkType { _<Nothing?>() }
    y checkType { _<Nothing>() }
}
