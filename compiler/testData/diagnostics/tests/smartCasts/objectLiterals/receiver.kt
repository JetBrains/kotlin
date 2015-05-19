// See KT-6293: Smart cast doesn't work after object literal
abstract class Runnable {
    abstract fun run()
}

fun foo(): Int {
    val c: Int? = null
    if (c is Int) {
        val d: Int = <!DEBUG_INFO_SMARTCAST!>c<!>
        object: Runnable() {
            override fun run() = Unit
        }.run()
        return <!DEBUG_INFO_SMARTCAST!>c<!> + d
    }
    else return -1
}
