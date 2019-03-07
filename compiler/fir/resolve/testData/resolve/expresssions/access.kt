
class Foo {
    val x = 1

    fun abc() = x

    fun cba() = abc()
}

class Bar {
    val x = ""

    fun Foo.abc() = x

    fun bar(): Bar = this

    operator fun String.plus(bar: Bar): String {
        return ""
    }

    fun Foo.check() = abc() + bar()
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
    val c = c

    abc()

    fun bcd() {}

    fun abc() {
        val a = d
        val b = a
        bcd()

        fun dcb() {}

        dcb()
    }

    dcb()

    abc()
}