data class A1(val prop1: String) {
    val prop2: String = "const2"
    var prop3: String = ""

    constructor(): this("default") {
        prop3 = "empty"
    }
    constructor(x: Int): this(x.toString()) {
        prop3 = "int"
    }

    fun f(): String = "$prop1#$prop2#$prop3"
}

data class A2 private () {
    var prop1: String = ""
    var prop2: String = "const2"
    var prop3: String = ""

    constructor(arg: String): this() {
        prop1 = arg
    }
    constructor(x: Double): this() {
        prop1 = "default"
        prop3 = "empty"
    }
    constructor(x: Int): this(x.toString()) {
        prop3 = "int"
    }

    fun f(): String = "$prop1#$prop2#$prop3"
}

fun box(): String {
    val a1x = A1("asd")
    if (a1x.f() != "asd#const2#") return "fail1: ${a1x.f()}"
    if (a1x.toString() != "A1(prop1=asd)") return "fail1s: ${a1x.toString()}"
    val a1y = A1()
    if (a1y.f() != "default#const2#empty") return "fail2: ${a1y.f()}"
    if (a1y.toString() != "A1(prop1=default)") return "fail2s: ${a1y.toString()}"
    val a1z = A1(5)
    if (a1z.f() != "5#const2#int") return "fail3: ${a1z.f()}"
    if (a1z.toString() != "A1(prop1=5)") return "fail3s: ${a1z.toString()}"

    val a2x = A2("asd")
    if (a2x.f() != "asd#const2#") return "fail4: ${a2x.f()}"
    val a2y = A2(123.0)
    if (a2y.f() != "default#const2#empty") return "fail5: ${a2y.f()}"
    val a2z = A2(5)
    if (a2z.f() != "5#const2#int") return "fail6: ${a2z.f()}"

    return "OK"
}
