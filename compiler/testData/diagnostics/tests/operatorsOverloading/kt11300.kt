class A {
    operator fun get(x: Int): Int = x
    fun set(<!UNUSED_PARAMETER!>x<!>: Int, <!UNUSED_PARAMETER!>y<!>: Int) {} // no `operator` modifier
}

fun main() {
    val a = A()
    <!OPERATOR_MODIFIER_REQUIRED!>a[1]<!>++
    <!OPERATOR_MODIFIER_REQUIRED!>a[1]<!> += 3
    <!OPERATOR_MODIFIER_REQUIRED!>a[1]<!> = a[1] + 3
}