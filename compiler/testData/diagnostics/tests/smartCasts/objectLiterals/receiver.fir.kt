// See KT-6293: Smart cast doesn't work after object literal
abstract class Runnable {
    abstract fun run()
}

fun foo(): Int {
    val c: Int? = null
    if (c is Int) {
        val d: Int = c
        object: Runnable() {
            override fun run() = Unit
        }.run()
        return c + d
    }
    else return -1
}
