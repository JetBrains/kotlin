// !DIAGNOSTICS: -UNUSED_PARAMETER
class A1 {
    constructor(): this()
}

class A2(x: Byte) {
    constructor(x1: Int): this(x1, 1)
    constructor(x1: Int, x2: Int): this(x1, x2, 2)
    constructor(x1: Int, x2: Int, x3: Int): this(x1)

    // delegating to previously declared cycle
    constructor(x1: Double): this(1)


    // delegating to cycle declared after
    constructor(x1: String): this(1L)

    constructor(x1: Long): this(x1, 1L)
    constructor(x1: Long, x2: Long): this(x1, x2, 2L)
    constructor(x1: Long, x2: Long, x3: Long): this(x1)

    // no cycle, just call to primary constuctor
    constructor(x1: Double, x2: Double): this(x1, x2, 1.0)
    constructor(x1: Double, x2: Double, x3: Double): this(x1, x2, x3, 1.0)
    constructor(x1: Double, x2: Double, x3: Double, x4: Double): this(1.toByte())

    constructor(): this("x", "y")

    constructor(x1: String, x2: String): this(x1, x2, "")
    constructor(x1: String, x2: String, x3: String): this(x1, x2)
}

open class B(x: Byte)
class A : B {
    // no cycle, just call to super constuctor
    constructor(x1: Double, x2: Double): this(x1, x2, 1.0)
    constructor(x1: Double, x2: Double, x3: Double): this(x1, x2, x3, 1.0)
    constructor(x1: Double, x2: Double, x3: Double, x4: Double): <!INAPPLICABLE_CANDIDATE!>super<!>(1.toByte())
}
