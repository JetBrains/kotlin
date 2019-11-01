
class Foo {
    val x = 1

    fun abc() = x

    fun cba() = abc()
}

class Bar {
    val x = ""

    // NB: unused
    fun Foo.abc() = x

    fun bar(): Bar = this

    // NB: unused
    operator fun String.plus(bar: Bar): String {
        return ""
    }

    // NB! abc() here is resolved to member Foo.abc(), and not to extension member of Bar
    fun Foo.check() = abc() <!INAPPLICABLE_CANDIDATE!>+<!> bar()

    // NB! + here is resolved to member String.plus (not to extension member above)
    fun Foo.check2() = "" + bar()
}

fun Foo.ext() = x

fun bar() {

}

fun buz() {
    bar()
}

fun f() {
    val a = 10
    val b = a
    val d = ""
    val c = <!UNRESOLVED_REFERENCE!>c<!>

    <!UNRESOLVED_REFERENCE!>abc<!>()

    fun bcd() {}

    fun abc() {
        val a = d
        val b = a
        bcd()

        fun dcb() {}

        dcb()
    }

    <!UNRESOLVED_REFERENCE!>dcb<!>()

    abc()
}