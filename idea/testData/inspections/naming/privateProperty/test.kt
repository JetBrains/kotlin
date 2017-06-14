val Foo: String = ""

var FOO_BAR: Int = 0

const val THREE = 3

val xyzzy = 1

fun foo() {
    val XYZZY = 1
    val BAR_BAZ = 2
}

object Foo {
    val Foo: String = ""

    var FOO_BAR: Int = 0
}

class D {
    private val _foo: String

    private val FOO_BAR: String

    companion object {
        val Foo: String = ""

        var FOO_BAR: Int = 0

        private val FOO_BAZ = 1
    }
}