// KOTLIN_CONFIGURATION_FLAGS: RUNTIME_STRING_CONCAT=enable
// JVM_TARGET: 9
fun box(a: String, b: String?) {
    val sb = StringBuilder();
    sb.append("123")
}

// 0 INVOKEDYNAMIC makeConcatWithConstants
// 1 append
// 0 stringPlus