// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
// FULL_JDK
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// 2 java/lang/invoke/LambdaMetafactory
// 0 \$sam\$
// 0 kotlin/jvm/internal/FunctionReference

import java.util.function.Consumer

var result = "Fail"

fun takeString(s: String) {
    result = s
}

fun <T> consume(c: Consumer<in T>, value: T) {
    c.accept(value)
}

fun box(): String {
    val map = HashMap<String, Int>()
    map["a"] = 1
    map.merge("a", 2, Int::plus)
    if (map["a"] != 3) return "Fail merge: ${map["a"]}"

    consume(::takeString, "OK")
    return result
}
