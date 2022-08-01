fun test() {
    val f = String::length
    val s = "hello"
    val g = s::<expr>length</expr>
    f() + g() + String::length.invoke(s) + s::length.invoke()
}