<!WRONG_MODIFIER_TARGET!>external annotation class A<!>

val x: Int
    <!WRONG_MODIFIER_TARGET!>external get() = noImpl<!>

class B

val B.x: Int
    <!WRONG_MODIFIER_TARGET!>external get() = noImpl<!>

class C {
    val a: Int
        <!WRONG_MODIFIER_TARGET!>external get() = noImpl<!>
}

external class D {
    val a: Int
        <!WRONG_MODIFIER_TARGET!>external get() = noImpl<!>
}