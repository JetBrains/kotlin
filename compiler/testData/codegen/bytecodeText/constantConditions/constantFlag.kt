// IGNORE_BACKEND: JVM_IR
// FILE: util.kt
const val FLAG = true
const val OTHER_FLAG = false

fun doStuff1() {}
fun doStuff2() {}
fun doStuff3() {}

inline fun doStuff1IfTrue(flag: Boolean) {
    if (flag) doStuff1()
}

inline fun doStuff2IfFalse(flag: Boolean) {
    if (!flag) doStuff2()
}

inline fun doStuff3IfComplex(flag: Boolean) {
    if (flag && !OTHER_FLAG) doStuff3()
}

// FILE: test.kt
fun test() {
    doStuff1IfTrue(FLAG)
    doStuff2IfFalse(FLAG)
    doStuff3IfComplex(FLAG)
}

// @TestKt.class:
// 0 FLAG
// 1 INVOKESTATIC UtilKt.doStuff1
// 0 INVOKESTATIC UtilKt.doStuff2
// 1 INVOKESTATIC UtilKt.doStuff3
// 0 ILOAD 0
// 0 IFEQ
// 0 IFNE
