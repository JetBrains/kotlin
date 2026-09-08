// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
// FULL_JDK
// SAM_CONVERSIONS: CLASS

import java.util.Comparator
import java.util.function.Consumer

var result = "Fail"

fun <T> foo(c: Consumer<in T>, value: T) {
    c.accept(value)
}

fun box(): String {
    foo<String>({ result = it }, "OK")
    if (result != "OK") return "Fail consumer: $result"

    val map = HashMap<Int, String>()
    val computed = map.computeIfAbsent(41) { (it + 1).toString() }
    if (computed != "42") return "Fail computeIfAbsent: $computed"

    val comparator = Comparator.comparingInt<String> { it.length }
    if (comparator.compare("a", "bb") >= 0) return "Fail comparingInt"

    return "OK"
}
