// !CHECK_TYPE

<!DEPRECATED_MODIFIER_PAIR!>open<!> <!DEPRECATED_MODIFIER_PAIR!>data<!> class A(private val x: Int)

class B : A(1) {
    fun component1(): String = ""
}

fun foo() {
    val b = B()
    checkSubtype<String>(b.component1())
    checkSubtype<Int>((checkSubtype<A>(b)).<!INVISIBLE_MEMBER!>component1<!>())
}
