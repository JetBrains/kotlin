// LANGUAGE: -JvmInlineValueClasses, +SealedInlineClasses
// IGNORE_BACKEND: JVM

var res = ""

sealed inline class I {
    init {
        res += "O"
    }
}

inline class IC(val s: Int): I() {
    init {
        res += "K"
    }
}

fun box(): String {
    IC(1)
    return res
}