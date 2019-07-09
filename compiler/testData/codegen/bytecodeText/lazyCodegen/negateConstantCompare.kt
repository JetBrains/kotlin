// !LANGUAGE: -InlineConstVals
// IGNORE_BACKEND: JVM_IR

const val one = 1
const val two = 2

fun test1() {
    if (!(one < two)) {
        val p = 1
    }
}

// 1 IF
