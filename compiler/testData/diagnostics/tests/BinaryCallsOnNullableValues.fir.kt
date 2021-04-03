// !WITH_NEW_INFERENCE
class A() {
    override fun equals(other : Any?) : Boolean = false
}

fun f(): Unit {
    var x: Int? = 1
    x = null
    x <!UNSAFE_OPERATOR_CALL!>+<!> 1
    x <!UNSAFE_CALL!>plus<!> 1
    x <!UNSAFE_OPERATOR_CALL!><<!> 1
    x <!UNRESOLVED_REFERENCE!>+=<!> 1

    x == 1
    x != 1

    <!EQUALITY_NOT_APPLICABLE!>A() == 1<!>

    <!EQUALITY_NOT_APPLICABLE!>x === "1"<!>
    <!EQUALITY_NOT_APPLICABLE!>x !== "1"<!>

    x === 1
    x !== 1

    x<!UNSAFE_OPERATOR_CALL!>..<!>2
    <!ARGUMENT_TYPE_MISMATCH!>x<!> in 1..2

    val y : Boolean? = true
    false || y
    y && true
    y && 1
}
