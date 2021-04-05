// !WITH_NEW_INFERENCE
class A() {
    override fun equals(other : Any?) : Boolean = false
}

fun f(): Unit {
    var x: Int? = 1
    x = null
    x <!NONE_APPLICABLE!>+<!> 1
    x <!NONE_APPLICABLE!>plus<!> 1
    x <!NONE_APPLICABLE!><<!> 1
    x <!UNRESOLVED_REFERENCE!>+=<!> 1

    x == 1
    x != 1

    A() == 1

    x === "1"
    x !== "1"

    x === 1
    x !== 1

    x<!NONE_APPLICABLE!>..<!>2
    <!ARGUMENT_TYPE_MISMATCH!>x<!> in 1..2

    val y : Boolean? = true
    false || y
    y && true
    y && 1
}
