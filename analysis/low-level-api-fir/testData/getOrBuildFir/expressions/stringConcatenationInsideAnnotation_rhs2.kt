annotation class Anno(val str: String)

@Anno("1" + "2" + <expr>"3"</expr>)
fun check() {
}
