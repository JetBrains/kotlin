// !DIAGNOSTICS: -DEPRECATION
object O

class TopLevel {
    external class <!NESTED_EXTERNAL_DECLARATION!>A<!>

    class B

    fun foo() = 23

    <!NESTED_EXTERNAL_DECLARATION!>external fun bar(): Int<!>

    <!NESTED_EXTERNAL_DECLARATION!>@native fun baz(): Int<!>

    val x = "a"

    <!NESTED_EXTERNAL_DECLARATION!>external val y: String<!>

    val O.u: String get() = "O.u"

    <!NESTED_EXTERNAL_DECLARATION!>external val O.v: String<!> get() = noImpl
}

external class TopLevelNative {
    external class <!NESTED_EXTERNAL_DECLARATION!>A<!>

    class B

    fun foo() = 23

    <!NESTED_EXTERNAL_DECLARATION!>external fun bar(): Int<!>

    val x = "a"

    <!NESTED_EXTERNAL_DECLARATION!>external val y: String<!>

    val O.u: String get() = "O.u"

    <!NESTED_EXTERNAL_DECLARATION!>external val O.v: String<!> get() = noImpl
}

fun topLevelFun() {
    external class <!NESTED_EXTERNAL_DECLARATION!>A<!>

    class B

    fun foo() = 23

    <!NESTED_EXTERNAL_DECLARATION!>external fun bar(): Int<!>
}

