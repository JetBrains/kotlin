fun test() {
    val f = String::length
    val s = "hello"
    val g = s::length
    f(s) + <expr>g</expr>() + String::length.invoke(s) + s::length.invoke()
}