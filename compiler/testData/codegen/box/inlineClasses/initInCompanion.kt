// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses
// CHECK_BYTECODE_LISTING
// IGNORE_BACKEND: JVM

var res = ""

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC(val s: String) {
    init {
        res += "IC"
    }

    companion object {
        init {
            res += "companion"
        }

        val ok = "OK"
    }
}

fun box(): String {
    IC.ok
    if (res != "companion") return "FAIL 1: $res"
    res = ""

    IC("")
    if (res != "IC") return "FAIL 2: $res"
    return "OK"
}