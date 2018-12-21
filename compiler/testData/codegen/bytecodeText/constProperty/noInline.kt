// !LANGUAGE: -InlineConstVals
// IGNORE_BACKEND: JVM_IR

const val z = 0

fun a() {
    val x = z
}

// 1 GETSTATIC NoInlineKt.z : I
