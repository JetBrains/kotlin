open data class A(private val x: Int)

class B : A(1) {
    fun component1(): String = ""
}

fun foo() {
    val b = B()
    b.component1() : String
    (b : A).<!INVISIBLE_MEMBER!>component1<!>() : Int
}
