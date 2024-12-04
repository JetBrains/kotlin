fun test() {
    val f = <expr>String</expr>::length
    val s = "hello"
    val g = s::length
    f() + g() + String::length.invoke(s) + s::length.invoke()
}