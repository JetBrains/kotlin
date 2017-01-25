package test

data class Modifiers(val x: Int) {
    tailrec fun sum(x: Long, sum: Long): Long {
        if (x == 0.toLong()) return sum
        return sum(x - 1, sum + x)
    }

    inline fun inlined(crossinline arg1: ()->Unit, noinline arg2: ()->Unit): Unit {}

    annotation class Ann
}
