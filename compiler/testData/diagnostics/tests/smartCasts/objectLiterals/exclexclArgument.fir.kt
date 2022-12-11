abstract class Runnable(val arg: Int) {
    abstract fun run(): Int
}

fun foo(): Int {
    val c: Int? = null
    val a: Int? = 1
    if (c is Int) {
        val k = object: Runnable(a!!) {
            override fun run() = arg
        }
        k.run()
        val d: Int = c
        return a + d
    }
    else return -1
}
