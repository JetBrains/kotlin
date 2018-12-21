// IGNORE_BACKEND: JVM_IR
// FILE: util.kt
const val MAGIC = 42

fun doStuffEq() {}
fun doStuffNotEq() {}
fun doStuffLt() {}
fun doStuffLe() {}
fun doStuffGt() {}
fun doStuffGe() {}

inline fun doStuffIfEq(i: Int) {
    if (i == MAGIC) doStuffEq()
}

inline fun doStuffIfNotEq(i: Int) {
    if (i != MAGIC) doStuffNotEq()
}

inline fun doStuffIfLt(i: Int) {
    if (i < MAGIC) doStuffLt()
}

inline fun doStuffIfLe(i: Int) {
    if (i <= MAGIC) doStuffLe()
}

inline fun doStuffIfGt(i: Int) {
    if (i > MAGIC) doStuffGt()
}

inline fun doStuffIfGe(i: Int) {
    if (i >= MAGIC) doStuffGe()
}

// FILE: test.kt
fun test() {
    doStuffIfEq(100)
    doStuffIfNotEq(100)
    doStuffIfLt(100)
    doStuffIfLe(100)
    doStuffIfGt(100)
    doStuffIfGe(100)
}

// @TestKt.class:
// 0 INVOKESTATIC UtilKt.doStuffEq
// 1 INVOKESTATIC UtilKt.doStuffNotEq
// 0 INVOKESTATIC UtilKt.doStuffLt
// 0 INVOKESTATIC UtilKt.doStuffLe
// 1 INVOKESTATIC UtilKt.doStuffGt
// 1 INVOKESTATIC UtilKt.doStuffGe
// 0 ILOAD 0
// 0 IFEQ
// 0 IFNE
// 0 IFLT
// 0 IFLE
// 0 IFGE
// 0 IFGT