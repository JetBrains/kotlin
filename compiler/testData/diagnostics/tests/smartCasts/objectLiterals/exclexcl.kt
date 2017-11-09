abstract class Runnable {
    abstract fun run()
}

fun foo(): Int {
    val c: Int? = null
    val a: Int? = 1
    if (c is Int) {
        val k = object: Runnable() {
            init {
                a!!.toInt()
            }
            override fun run() = Unit
        }
        k.run()
        val d: Int = <!DEBUG_INFO_SMARTCAST!>c<!>
        // a is not null because of k constructor, but we do not know it
        return a <!UNSAFE_OPERATOR_CALL!>+<!> d
    }
    else return -1
}
