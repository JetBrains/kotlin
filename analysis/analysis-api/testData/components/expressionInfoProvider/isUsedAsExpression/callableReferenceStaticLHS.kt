fun test() {
    val f = <expr>String::length</expr>
    val s = "hello"
    val g = s::length</expr>
    f() + g() + String::length.invoke(s) + s::length.invoke()
}