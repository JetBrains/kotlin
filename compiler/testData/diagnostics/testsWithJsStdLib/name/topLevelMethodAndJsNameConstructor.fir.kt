package foo

class A(val x: String) {
    @JsName("aa") constructor(x: Int) : this("int $x")
}

fun aa() {}
