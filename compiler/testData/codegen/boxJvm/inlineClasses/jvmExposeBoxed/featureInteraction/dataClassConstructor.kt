// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING
// JVM_EXPOSE_BOXED

// FILE: IC.kt
@JvmInline
value class StringWrapper constructor(val s: String)

@JvmInline
value class StringWrapperNullable constructor(val s: String?)

data class Test(val s: StringWrapper) {
    fun ok(): String = s!!.s!!
}

data class TestNullable(val s: StringWrapper?) {
    fun ok(): String = s!!.s!!
}

data class TestNullableNullable(val s: StringWrapperNullable?) {
    fun ok(): String = s!!.s!!
}

// FILE: Main.java
public class Main {
    public String test() {
        return new Test(new StringWrapper("OK")).ok();
    }
    public String testNullable() {
        return new TestNullable(new StringWrapper("OK")).ok();
    }
    public String testNullableNullable() {
        return new TestNullableNullable(new StringWrapperNullable("OK")).ok();
    }
}

// FILE: Box.kt
fun box(): String {
    var res = Main().test()
    if (res != "OK") return "FAIL 1: $res"
    res = Main().testNullable()
    if (res != "OK") return "FAIL 2: $res"
    res = Main().testNullableNullable()
    if (res != "OK") return "FAIL 3: $res"
    return res
}
