// CHECK_BYTECODE_LISTING
// LANGUAGE: -JvmInlineValueClasses, +SealedInlineClasses
// IGNORE_BACKEND: JVM

sealed inline class IC

inline class ICString(val s: String): IC()

object ICO: IC()

fun check(ic: IC): String = when(ic) {
    is ICString -> ic.s
    ICO -> "FAIL"
}

fun box() = check(ICString("OK"))