fun test() {
    val f = String::length
    val s = "hello"
    val g = s::length
    <expr>f</expr>(s) + g() + String::length.invoke(s) + s::length.invoke()
}