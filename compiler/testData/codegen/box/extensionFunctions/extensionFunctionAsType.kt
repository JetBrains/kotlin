fun foo(x: Int.(y: String) -> Unit) {
    1.x("")
    x(1, "")
}

fun bar(): Int.(y: String) -> Unit {
    return { y: String ->  }
}

class B(foo: Int.(y: String) -> Unit) {
    init {
        4.foo("")
    }
}

fun test(){
    foo { y: String -> y.length }

    val t = bar()
    1.t("")
    t(1, "")

    B { y: String -> y.length }
}

fun box(): String {
    test()
    return "OK"
}