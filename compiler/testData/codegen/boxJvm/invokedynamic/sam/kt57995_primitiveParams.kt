// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
// FULL_JDK
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// 3 java/lang/invoke/LambdaMetafactory
// 0 \$sam\$

import java.util.Comparator

fun box(): String {
    val map = HashMap<Int, String>()
    val computed = map.computeIfAbsent(41) { (it + 1).toString() }
    if (computed != "42") return "Fail computeIfAbsent: $computed"

    val comparator = Comparator.comparingInt<String> { it.length }
    if (comparator.compare("a", "bb") >= 0) return "Fail comparingInt"

    val counters = HashMap<String, Int>()
    counters["a"] = 1
    val merged = counters.merge("a", 2) { old, new -> old + new }
    if (merged != 3) return "Fail merge: $merged"

    return "OK"
}
