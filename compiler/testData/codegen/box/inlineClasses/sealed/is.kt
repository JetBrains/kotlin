// LANGUAGE: -JvmInlineValueClasses, +SealedInlineClasses
// IGNORE_BACKEND: JVM

sealed inline class IC

inline class ICAny(val s: Any): IC()

object ICO: IC()

fun check(ic: IC): String = when(ic) {
    is ICAny -> ic.s as String
    ICO -> "K"
}

fun box() = check(ICAny("O")) + check(ICO)