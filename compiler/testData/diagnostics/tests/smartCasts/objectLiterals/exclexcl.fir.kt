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
        val d: Int = c
        // a is not null because of k constructor, but we do not know it
        return a + d
    }
    else return -1
}
