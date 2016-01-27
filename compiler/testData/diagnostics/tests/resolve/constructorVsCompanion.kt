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

val a = <!NO_COMPANION_OBJECT!>A<!>
val <!EXPOSED_PROPERTY_TYPE!>b<!> = <!INVISIBLE_MEMBER!>B<!>
val c = <!NO_COMPANION_OBJECT!>C<!>
val d = D
val e = E(42)