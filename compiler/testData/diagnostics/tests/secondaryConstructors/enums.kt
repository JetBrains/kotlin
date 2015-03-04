// !DIAGNOSTICS: -UNUSED_PARAMETER
enum class A {
    W: A(1) X: A(1, 2) Y: A(3.0) Z: A("") E: A()

    constructor() {}
    constructor(x: Int) {}
    constructor(x: Int, y: Int): this(x+y) {}
    constructor(x: Double): this(x.toInt(), 1) {}
    constructor(x: String): <!DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR!>super<!>(x, 1) {}
}

enum class B(x: Int) {
    W: B(1) X: B(1, 2) Y: B(3.0) Z: B("")

    constructor(x: Int, y: Int): this(x+y) {}
    constructor(x: Double): this(x.toInt(), 1) {}
    constructor(x: String): <!DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR!>super<!>(x, 1) {}
}

enum class C {
    EMPTY: C() // may be we should avoid explicit call here
    constructor() {}
}

enum class D(val prop: Int) {
    X: D(123) {
        override fun f() = 1
    }
    Y: D() {
        override fun f() = prop
    }
    Z: D("abc") {
        override fun f() = prop
    }

    constructor(): this(1) {}
    constructor(x: String): this(x.length()) {}

    abstract fun f(): Int
}
