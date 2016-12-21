external fun foo(): Int = noImpl

external fun bar(): Unit {
    noImpl
}

external fun baz(): Int = <!WRONG_BODY_OF_EXTERNAL_DECLARATION!>23<!>

external fun f(x: Int, y: String = noImpl): Unit

external fun g(x: Int, y: String = <!WRONG_DEFAULT_VALUE_FOR_EXTERNAL_FUN_PARAMETER!>""<!>): Unit

external var a: Int
    get() = noImpl
    set(value) {
        noImpl
    }

external val b: Int
    get() = <!WRONG_BODY_OF_EXTERNAL_DECLARATION!>23<!>

external val c: Int = noImpl

external val d: Int = <!WRONG_INITIALIZER_OF_EXTERNAL_DECLARATION!>23<!>

external class C {
    fun foo(): Int = noImpl

    fun bar(): Int = <!WRONG_BODY_OF_EXTERNAL_DECLARATION!>23<!>
}
