class A(val x: String = "def_x", val y: String = "1") {
    constructor(x: String, y: Int): this(x, y.toString()) {}
    constructor(x: Double): this(x.toString(), "def_y") {}
    override fun toString() = "$x#$y"
}

fun box(): String {
    val test1 = WithPrimary.test1().toString()
    if (test1 != "123#abc") return "fail1: $test1"

    val test2 = WithPrimary.test2().toString()
    if (test2 != "def_x#1") return "fail2: $test2"

    val test3 = WithPrimary.test3().toString()
    if (test3 != "123#456") return "fail3: $test3"

    val test4 = WithPrimary.test4().toString()
    if (test4 != "1.0#def_y") return "fail4: $test4"

    return "OK"
}
