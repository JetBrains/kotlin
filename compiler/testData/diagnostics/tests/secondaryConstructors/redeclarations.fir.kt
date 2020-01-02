// !DIAGNOSTICS: -UNUSED_PARAMETER

class A(x: String = "", y: String = "") {
    constructor(x: String, y: String): this(x, y)
    constructor(): this("", "")
    constructor(): this("", "")
}

class B {
    constructor(x: Int)
}

fun B(x: Int) {}

class Outer {
    class A(x: String = "", y: String = "") {
        constructor(x: String, y: String): this(x, y)
        constructor(): this("", "")
        constructor(): this("", "")
    }

    class B {
        constructor(x: Int)
    }

    fun B(x: Int) {}
}