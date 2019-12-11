// !WITH_NEW_INFERENCE
// !CHECK_TYPE
val x get() = null
val y get() = null!!

fun foo() {
    x checkType { <!UNRESOLVED_REFERENCE!>_<!><Nothing?>() }
    y checkType { <!UNRESOLVED_REFERENCE!>_<!><Nothing>() }
}
