// FILE: A.kt

open class A {
    val prop: String
    constructor(x1: String, x2: String = "abc") {
        prop = "$x1#$x2"
    }
    constructor(x1: Long) {
        prop = "$x1"
    }
}

// FILE: B.kt

class B1() : A("123") {
    constructor(x1: Int): this() {}
}

class B2 : A {
    constructor(x1: String): super(x1) {}
    constructor(): this("empty") {}
    constructor(x1: Int): super(x1.toLong()) {}
}

fun box(): String {
    val b1 = B1()
    if (b1.prop != "123#abc") return "fail1: ${b1.prop}"
    val b2 = B1(456)
    if (b2.prop != "123#abc") return "fail2: ${b2.prop}"
    val b3 = B2("cde")
    if (b3.prop != "cde#abc") return "fail3: ${b3.prop}"
    val b4 = B2()
    if (b4.prop != "empty#abc") return "fail4: ${b4.prop}"
    val b5 = B2(789)
    if (b5.prop != "789") return "fail5: ${b5.prop}"

    return "OK"
}
