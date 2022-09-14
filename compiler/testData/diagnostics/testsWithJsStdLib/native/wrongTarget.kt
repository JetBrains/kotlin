// FIR_IDENTICAL
external annotation class <!WRONG_EXTERNAL_DECLARATION!>A(val x: Int)<!>

val x: Int
    <!WRONG_EXTERNAL_DECLARATION!>external get()<!> = definedExternally

class B

<!WRONG_EXTERNAL_DECLARATION!>val B.x: Int<!>
    <!WRONG_EXTERNAL_DECLARATION!>external get()<!> = definedExternally

class C {
    val a: Int
        <!WRONG_EXTERNAL_DECLARATION!>external get()<!> = definedExternally
}

external class D {
    val a: Int
        <!WRONG_EXTERNAL_DECLARATION!>external get()<!> = definedExternally
}

external data class <!WRONG_EXTERNAL_DECLARATION!>E(val x: Int)<!>