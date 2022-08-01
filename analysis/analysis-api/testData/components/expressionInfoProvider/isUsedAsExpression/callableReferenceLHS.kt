fun test() {
    val f = String::length
    val s = "hello"
    val g = <expr>s</expr>::length
    f() + g() + String::length.invoke(s) + s::length.invoke()
}