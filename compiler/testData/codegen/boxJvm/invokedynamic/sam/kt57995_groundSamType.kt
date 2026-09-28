// TARGET_BACKEND: JVM
// FULL_JDK
// JVM_TARGET: 1.8
// WITH_STDLIB
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 4 java/lang/invoke/LambdaMetafactory

import java.util.Comparator
import java.util.HashMap
import java.util.function.Consumer

fun <T> consume(block: Consumer<in T>) {}

fun use() {
    consume<String> { }
}

fun useComparingInt(): Boolean {
    val comparator = Comparator.comparingInt<String> { it.length }
    return comparator.compare("a", "bb") < 0
}

fun useMapCompute(): Boolean {
    val map = HashMap<String, Int>()
    map["value"] = 2
    return map.compute("value") { key, value -> key.length + (value ?: 0) } == 7
}

fun useMapComputeIfAbsent(): Boolean {
    val map = HashMap<String, Int>()
    return map.computeIfAbsent("value") { it.length } == 5
}

fun box(): String {
    use()
    if (!useComparingInt()) return "Fail comparingInt"
    if (!useMapCompute()) return "Fail compute"
    if (!useMapComputeIfAbsent()) return "Fail computeIfAbsent"
    return "OK"
}
