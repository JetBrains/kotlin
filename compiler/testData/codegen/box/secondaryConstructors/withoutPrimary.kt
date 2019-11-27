// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// FILE: WithoutPrimary.java

class WithoutPrimary {
    public static A test1() {
        return new A("123", "abc");
    }
    public static A test3() {
        return new A("123", 456);
    }
    public static A test4() {
        return new A(1.0);
    }
}

// FILE: WithoutPrimary.kt

class A {
    val x: String 
    val y: String
    constructor(x: String, y: String) {
        this.x = x
        this.y = y
    }
    constructor(x: String = "def_x", y: Int = 1): this(x, y.toString()) {}
    constructor(x: Double): this(x.toString(), "def_y") {}
    override fun toString() = "$x#$y"
}

fun box(): String {
    val test1 = WithoutPrimary.test1().toString()
    if (test1 != "123#abc") return "fail1: $test1"

    val test3 = WithoutPrimary.test3().toString()
    if (test3 != "123#456") return "fail3: $test3"

    val test4 = WithoutPrimary.test4().toString()
    if (test4 != "1.0#def_y") return "fail4: $test4"

    return "OK"
}
