// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING
// JVM_EXPOSE_BOXED

// FILE: IC.kt
@JvmInline
value class StringWrapper constructor(val s: String)

class Test(val s: StringWrapper) {
    fun ok(): String = s.s
}

class TestNullable(val s: StringWrapper?) {
    fun ok(): String = s!!.s
}

// FILE: Main.java
public class Main {
    public String test() {
        return new Test(new StringWrapper("OK")).ok();
    }

    public String testNullable() {
        return new TestNullable(new StringWrapper("OK")).ok();
    }
}

// FILE: Box.kt
fun box(): String {
    var res = Main().test()
    if (res != "OK") return "FAIL 1: $res"
    res = Main().testNullable()
    if (res != "OK") return "FAIL 2: $res"
    return res
}
