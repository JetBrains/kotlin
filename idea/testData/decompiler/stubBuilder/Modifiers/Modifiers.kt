package test

data class Modifiers(val x: Int) {
    external fun extFun()

    var extVar: Int = 1
        external get
        external set

    tailrec fun sum(x: Long, sum: Long): Long {
        if (x == 0.toLong()) return sum
        return sum(x - 1, sum + x)
    }

    inline fun inlined(crossinline arg1: ()->Unit, noinline arg2: ()->Unit): Unit {}

    annotation class Ann
}