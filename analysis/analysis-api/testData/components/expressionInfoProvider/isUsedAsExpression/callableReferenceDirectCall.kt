fun test() {
    val f = String::length
    val s = "hello"
    val g = s::length
    f(s) + g() + String::length.invoke(s) + <expr>s::length</expr>.invoke()
}