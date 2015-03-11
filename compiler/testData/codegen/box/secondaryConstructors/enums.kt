enum class A1(val prop1: String) {
    X: A1("asd")
    Y: A1() {
        override fun f() = super.f() + "#Y"
    }
    Z: A1(5)

    val prop2: String = "const2"
    var prop3: String = ""

    constructor(): this("default") {
        prop3 = "empty"
    }
    constructor(x: Int): this(x.toString()) {
        prop3 = "int"
    }

    open fun f(): String = "$prop1#$prop2#$prop3"
}

enum class A2 {
    val prop1: String
    X: A2("asd")
    Y: A2() {
        override fun f() = super.f() + "#Y"
    }
    Z: A2(5)

    val prop2: String = "const2"
    var prop3: String = ""

    constructor(arg: String) {
        prop1 = arg
    }
    constructor() {
        prop1 = "default"
        prop3 = "empty"
    }
    constructor(x: Int): this(x.toString()) {
        prop3 = "int"
    }

    open fun f(): String = "$prop1#$prop2#$prop3"
}

fun box(): String {
    val a1x = A1.X
    if (a1x.f() != "asd#const2#") return "fail1: ${a1x.f()}"
    val a1y = A1.Y
    if (a1y.f() != "default#const2#empty#Y") return "fail2: ${a1y.f()}"
    val a1z = A1.Z
    if (a1z.f() != "5#const2#int") return "fail3: ${a1z.f()}"

    val a2x = A2.X
    if (a2x.f() != "asd#const2#") return "fail4: ${a2x.f()}"
    val a2y = A2.Y
    if (a2y.f() != "default#const2#empty#Y") return "fail5: ${a2y.f()}"
    val a2z = A2.Z
    if (a2z.f() != "5#const2#int") return "fail6: ${a2z.f()}"

    return "OK"
}
