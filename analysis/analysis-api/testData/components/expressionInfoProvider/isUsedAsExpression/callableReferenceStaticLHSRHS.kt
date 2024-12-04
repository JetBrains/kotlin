fun test() {
    val f = String::<expr>length</expr>
    val s = "hello"
    val g = s::length
    f() + g() + String::length.invoke(s) + s::length.invoke()
}