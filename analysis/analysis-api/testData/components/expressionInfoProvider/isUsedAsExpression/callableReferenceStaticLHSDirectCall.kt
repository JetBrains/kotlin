fun test() {
    val f = String::length
    val s = "hello"
    val g = s::length
    f(s) + g() + <expr>String::length</expr>.invoke(s) + s::length.invoke()
}