// !LANGUAGE: -InlineConstVals
// IGNORE_BACKEND: JVM_IR

const val z = 0

fun a() {
    if (z == 2) {
    }
}

// 1 GETSTATIC NoInlineInCmpKt.z : I
