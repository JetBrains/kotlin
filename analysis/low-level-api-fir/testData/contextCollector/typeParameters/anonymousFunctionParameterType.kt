package test

fun test() {
    val anon = fun <T> (x: <expr>T</expr>): T {
        val y: T = x
        return y
    }
}