class A private constructor()

class B {
    private companion object
}

class C(val x: Int)

class D private constructor() {
    companion object
}

class E private constructor() {
    companion object {
        operator fun invoke(x: Int) = x
    }
}

val a = <!INVISIBLE_MEMBER, FUNCTION_CALL_EXPECTED!>A<!>
<!EXPOSED_PROPERTY_TYPE!>val b = <!INVISIBLE_MEMBER!>B<!><!>
val c = <!NO_VALUE_FOR_PARAMETER, FUNCTION_CALL_EXPECTED!>C<!>
val d = D
val e = E(42)