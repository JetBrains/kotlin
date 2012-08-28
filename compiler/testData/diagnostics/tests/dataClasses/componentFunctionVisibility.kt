open data class A(private val x: Int, protected val y: String, public val z: Any)

fun foo(a: A) {
    a.<!INVISIBLE_MEMBER!>component1<!>()
    a.<!INVISIBLE_MEMBER!>component2<!>()
    a.component3()
}

class B : A(42, "", "") {
    fun foo() {
        this.<!INVISIBLE_MEMBER!>component1<!>()
        this.component2()
        this.component3()
    }
}
