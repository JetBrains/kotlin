// ISSUE: KT-67581

// MODULE: common
// FILE: common.kt
expect abstract class Memory

// MODULE: jvm()()(common)
// FILE: kotlin.kt

actual typealias <!ACTUAL_WITHOUT_EXPECT!>Memory<!> = J

// FILE: J.java
public abstract sealed class J permits J1 {
}

// FILE: J1.java
public final class J1 extends J {
}
