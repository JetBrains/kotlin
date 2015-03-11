import java.lang

class B1() : A("123") {
    constructor(x1: Int): this() {}
}

class B2 : A {
    constructor(x1: String): super(x1) {}
    constructor(): this("empty") {}
    constructor(x1: Int): super(x1.toLong()) {}
}

fun main(args: Array<String>) {
    val b1 = B1()
    if (b1.prop != "123#abc") throw AssertionError("fail1: ${b1.prop}")
    val b2 = B1(456)
    if (b2.prop != "123#abc") throw AssertionError("fail2: ${b2.prop}")

    val b3 = B2("cde")
    if (b3.prop != "cde#abc") throw AssertionError("fail3: ${b3.prop}")
    val b4 = B2()
    if (b4.prop != "empty#abc") throw AssertionError("fail4: ${b4.prop}")
    val b5 = B2(789)
    if (b5.prop != "789") throw AssertionError("fail5: ${b5.prop}")
}
