external annotation class <!WRONG_EXTERNAL_DECLARATION!>A(val x: Int)<!>

val x: Int
    <!WRONG_EXTERNAL_DECLARATION!>external get()<!> = noImpl

class B

val B.x: Int
    <!WRONG_EXTERNAL_DECLARATION!>external get()<!> = noImpl

class C {
    val a: Int
        <!WRONG_EXTERNAL_DECLARATION!>external get()<!> = noImpl
}

external class D {
    val a: Int
        <!WRONG_EXTERNAL_DECLARATION!>external get()<!> = noImpl
}

external data class <!WRONG_EXTERNAL_DECLARATION!>E(val x: Int)<!>