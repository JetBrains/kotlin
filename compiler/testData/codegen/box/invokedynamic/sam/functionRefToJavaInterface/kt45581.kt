// TARGET_BACKEND: JVM
// WITH_STDLIB
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FULL_JDK
// IGNORE_DEXING
//  ^ D8 fails with AssertionError, possible reason: only Kotlin output files are passed to D8

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

// FILE: kt45581.kt

fun box(): String = J.bar(emptySet<String>()::contains)

// FILE: J.java

import java.util.function.Predicate;

public class J {
    public static String bar(Predicate<String> predicate) {
        return "OK";
    }
}
