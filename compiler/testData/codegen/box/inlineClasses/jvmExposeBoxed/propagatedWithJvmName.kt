// WITH_STDLIB
// TARGET_BACKEND: JVM
// CHECK_BYTECODE_LISTING
// FILE: Test.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class StringWrapper(val s: String)

@JvmExposeBoxed
class Implicit {
    @JvmName("foo11")
    fun foo1(sw: StringWrapper): Int = 42
}

@JvmExposeBoxed("createSW")
fun create(s: String): StringWrapper = StringWrapper(s)

// FILE: Main.java
public class Main {
    public int test() {
        return new Implicit().foo11(TestKt.createSW("OK"));
    }
}

// FILE: box.kt
fun box(): String {
    val res = Main().test()
    if (res != 42) return "FAIL $res"
    return "OK"
}
