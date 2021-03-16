// FIR_IDENTICAL
fun f() {
    val g = 3
    <error>g</error>(object : Any() {})
}
