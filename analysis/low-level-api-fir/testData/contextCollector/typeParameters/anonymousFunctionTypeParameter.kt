package test

fun test() {
    val anon = fun <<expr>T</expr>> (x: T): T {
        val y: T = x
        return y
    }
}
