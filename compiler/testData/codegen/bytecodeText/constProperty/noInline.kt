// !LANGUAGE: -InlineConstVals
// IGNORE_BACKEND_FIR: JVM_IR

// Fir2Ir IrConstTransformer inline everything it can unconditionally
// No fix atm, we don't have to support this feature
const val z = 0

fun a() {
    val x = z
}

// 1 GETSTATIC NoInlineKt.z : I
