package test

fun test() {
    val anon = fun <T> (x: T): T {
        val y: <expr>T</expr> = x
        return y
    }
}