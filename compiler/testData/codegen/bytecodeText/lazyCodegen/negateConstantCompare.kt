// !LANGUAGE: -InlineConstVals
// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: don't support legacy feature

const val one = 1
const val two = 2

fun test1() {
    if (!(one < two)) {
        val p = 1
    }
}

// 1 IF
