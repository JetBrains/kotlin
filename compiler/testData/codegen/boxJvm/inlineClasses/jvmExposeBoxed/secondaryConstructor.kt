// WITH_STDLIB
// TARGET_BACKEND: JVM
// FILE: Test.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class MyUint(val a: UInt)

@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed
class MyClass(val a: MyUint) {
    constructor(param1: UInt, param2: String): this(MyUint(param1 + 3u)) {
    }
}

@JvmExposeBoxed("createUInt")
fun create(i: Int): UInt = i.toUInt()

// FILE: Main.java
public class Main {
    public MyUint test() {
        return new MyClass(TestKt.createUInt(42), "OK").getA();
    }
}

// FILE: box.kt
fun box(): String {
    val res = Main().test().a
    if (res != 45u) return "FAIL: $res"
    return "OK"
}
