// !LANGUAGE: -InlineConstVals
// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: don't support legacy feature

const val z = 0

fun a() {
    if (z == 2) {
    }
}

// 1 GETSTATIC NoInlineInCmpKt.z : I
