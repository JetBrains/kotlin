// IGNORE_FE10
fun f(s: String, action: (String.() -> Unit)?) {
    <expr>s.action</expr>?.let { it() }
}
