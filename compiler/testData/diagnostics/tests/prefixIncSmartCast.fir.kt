// Changed in K2, see KT-57178

open class I {
    operator fun inc(): ST = ST()
}

class ST : I()

fun main() {
    var local = I()
    val x: ST = ++local
    val y: ST = local
}
