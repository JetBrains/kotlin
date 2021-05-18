// !WITH_NEW_INFERENCE

class A {
    operator fun get(x: Int): Int = x
    fun set(x: Int, y: Int) {} // no `operator` modifier
}

fun main() {
    val a = A()
    <!OPERATOR_MODIFIER_REQUIRED!>a[1]<!>++
    //fir prefers plusAssign call if neither get+set nor plusAssign resolverd
    //hence UNRESOLVED_REFERENCE here
    a[1] <!UNRESOLVED_REFERENCE!>+=<!> 3
    <!OPERATOR_MODIFIER_REQUIRED!>a[1]<!> = a[1] + 3
}
