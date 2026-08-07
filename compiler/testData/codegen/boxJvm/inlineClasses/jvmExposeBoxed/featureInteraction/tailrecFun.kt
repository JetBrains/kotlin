// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Count(val value: Int)

// The mangled implementation is lowered into a loop; the boxed variant has to delegate to it rather than
// recurse through the boxed signature.
@JvmExposeBoxed
tailrec fun countDown(count: Count): Count =
    if (count.value <= 0) count else countDown(Count(count.value - 1))

// FILE: Main.java
public class Main {
    public int test() {
        return ICKt.countDown(new Count(5)).getValue();
    }
}

// FILE: Box.kt
fun box(): String {
    val res = Main().test()
    if (res != 0) return "FAIL: $res"
    return "OK"
}
