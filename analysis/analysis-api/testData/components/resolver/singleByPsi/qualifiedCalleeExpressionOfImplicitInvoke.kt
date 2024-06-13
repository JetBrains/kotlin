interface A {
    val f: () -> Unit
}
fun test(a: A) {
    a.<expr>f</expr>()
}