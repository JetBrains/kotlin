// FIR_COMPARISON
fun f() {
    val g = 3
    <error>g</error>(object : Any() {})
}