// JVM_DEFAULT_MODE: enable
// WITH_STDLIB
// CHECK_BYTECODE_LISTING
// LANGUAGE: +ImplicitJvmExposeBoxed
// TARGET_BACKEND: JVM_IR

// checking that TestClass has correctly generated mangled and exposed overrides

// FILE: Test.kt
interface Test {
    fun test(p: UInt): UInt
    val foo: UInt?
}

open class TestClass1 : Test {
    override fun test(p: UInt): UInt {
        return p
    }
    override val foo: UInt? get() = 1u
}

class TestClass: TestClass1() {
    override fun test(p: UInt): UInt {
        return super.test(p)
    }

    override val foo: UInt?
        get() = super.foo
}

// FILE: TestJava.java
public class TestJava {
    public static kotlin.UInt test1(kotlin.UInt u) {
        return new TestClass().test(u);
    }
    public static kotlin.UInt test2() {
        return new TestClass().getFoo();
    }
}

// FILE: box.kt
fun box(): String {
    var res = TestJava.test1(42u)
    if (res != 42u) return "FAIL $res"
    res = TestJava.test2()
    if (res != 1u) return "FAIL $res"
    return "OK"
}