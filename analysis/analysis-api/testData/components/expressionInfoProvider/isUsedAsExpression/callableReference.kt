fun test() {
    val f = String::length
    val s = "hello"
    val g = <expr>s::length</expr>
    f(s) + g() + String::length.invoke(s) + s::length.invoke()
}