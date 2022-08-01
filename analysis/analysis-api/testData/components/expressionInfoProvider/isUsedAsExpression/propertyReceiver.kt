fun Any.hashKode(): Int = 404

fun test(b: Boolean): Int {
    val n: Int = <expr>b</expr>.hashKode()
    return n * 2
}