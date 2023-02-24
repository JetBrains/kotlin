// FIR_IDENTICAL
// ISSUE: KT-56942
// FILE: EnumJava.java
public enum EnumJava {
    JAVA_ONE, JAVA_TWO;

    public static EnumJava provide() { return EnumJava.JAVA_ONE; }
}

// FILE: EnumProviderJava.java
public class EnumProviderJava {
    public static EnumJava provide() { return EnumJava.JAVA_ONE; }
}


// FILE: main.kt
enum class EnumKotlin {
    KOTLIN_ONE, KOTLIN_TWO;

    companion object {
        fun provide(): EnumKotlin = EnumKotlin.KOTLIN_ONE
    }
}

fun test_1(ejp: EnumJava, ekp: EnumKotlin) {
    <!NO_ELSE_IN_WHEN!>when<!> (ejp) {
        EnumJava.JAVA_ONE -> {}
    }

    <!NO_ELSE_IN_WHEN!>when<!> (ekp) {
        EnumKotlin.KOTLIN_ONE -> {}
    }

    <!NO_ELSE_IN_WHEN!>when<!> (EnumJava.provide()) {
        EnumJava.JAVA_ONE -> {}
    }

    <!NO_ELSE_IN_WHEN!>when<!> (EnumKotlin.provide()) {
        EnumKotlin.KOTLIN_ONE -> {}
    }
}

fun test_2(ejp: EnumJava, ekp: EnumKotlin) {
    <!NO_ELSE_IN_WHEN!>when<!> (ejp) {}

    <!NO_ELSE_IN_WHEN!>when<!> (ekp) {}

    <!NO_ELSE_IN_WHEN!>when<!> (EnumProviderJava.provide()) {}

    <!NO_ELSE_IN_WHEN!>when<!> (EnumKotlin.provide()) {}
}
