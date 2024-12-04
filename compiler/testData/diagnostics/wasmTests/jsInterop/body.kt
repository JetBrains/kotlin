// FIR_IDENTICAL
external fun foo(): Int = definedExternally

external fun bar(): Unit {
    definedExternally
}

external fun baz(): Int = <!WRONG_BODY_OF_EXTERNAL_DECLARATION!>23<!>

external fun f(x: Int, y: String = definedExternally): Unit

external fun g(x: Int, y: String = <!WRONG_DEFAULT_VALUE_FOR_EXTERNAL_FUN_PARAMETER!>""<!>): Unit

external var a: Int
    get() = definedExternally
    set(value) {
        definedExternally
    }

external val b: Int
    get() = <!WRONG_BODY_OF_EXTERNAL_DECLARATION!>23<!>

external val c: Int = definedExternally

external val d: Int = <!WRONG_INITIALIZER_OF_EXTERNAL_DECLARATION!>23<!>

external class C {
    fun foo(): Int = definedExternally

    fun bar(): Int = <!WRONG_BODY_OF_EXTERNAL_DECLARATION!>23<!>
}