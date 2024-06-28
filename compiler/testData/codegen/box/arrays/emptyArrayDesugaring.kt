// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

// Checks if emptyArray call has been lowered

fun box(): String {
    emptyArray<String>()
    return "OK"
}

// CHECK_BYTECODE_TEXT
// 0 ISTORE 0