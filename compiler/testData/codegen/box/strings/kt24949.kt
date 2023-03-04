// TARGET_BACKEND: JVM_IR

fun testShortConcat(): String {
    val c = "a"
    val sb = StringBuilder()
    sb.append(c + c + c)
    return sb.toString()
}

fun testLongConcat(): String {
    val c = "a"
    val sb = StringBuilder()
    sb.append(c + c + c + c + c + c + c + c + c + c + c + c + c + c + c + c + c + c + c + c + c + c + c + c + c + c + c + c + c + c)
    return sb.toString()
}

fun box(): String {
    if (testShortConcat() != "aaa") return "Fail 1"
    if (testLongConcat() != "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa") return "Fail 2"
    return "OK"
}

// CHECK_BYTECODE_TEXT
// 2 NEW java/lang/StringBuilder