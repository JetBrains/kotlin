// TARGET_BACKEND: JVM_IR
// ISSUE: KT-56548

// MODULE: m1
// FILE: SomeWrapper.java
public final class SomeWrapper {
    public record SomeRecord(String a, String b) {}
}

// MODULE: m2(m1)
// FILE: test.kt
import SomeWrapper.SomeRecord

fun testBinary(s: String): String {
    val record = SomeRecord(s, "K")
    return record.a + record.b
}

fun box(): String {
    return testBinary("O")
}
