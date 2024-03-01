open class I {
    operator fun inc(): ST = ST()
}

class ST : I()

fun main() {
    var local = I()
    val x: ST = <!INITIALIZER_TYPE_MISMATCH!>local++<!>
    val y: ST = local
}
