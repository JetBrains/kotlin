// !CHECK_TYPE
val x get() = null
val y get() = null!!

fun foo() {
    x checkType { _<Nothing?>() }
    y checkType { _<Nothing>() }
}
