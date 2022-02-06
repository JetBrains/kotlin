// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter, +SealedInlineClasses
// IGNORE_BACKEND: JVM

sealed inline class IC {
    override fun toString(): String = "FAIL"
}

inline class ICString(val s: String): IC() {
    override fun toString(): String = s
}

object ICO: IC()

fun toString(ic: IC): String = ic.toString()

fun box(): String {
    val res = toString(ICO)
    if (res != "ICO()") return res
    return "OK"
}