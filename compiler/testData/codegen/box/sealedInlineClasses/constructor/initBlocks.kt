// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +SealedInlineClasses

var res = ""

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class I {
    init {
        res += "O"
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC(val s: Int): I() {
    init {
        res += "K"
    }
}

fun box(): String {
    IC(1)
    return res
}