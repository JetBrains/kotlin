// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Foo<T: Char>(val c: T) {
    companion object {
        val prop = "O"
        const val constVal = 1
        fun funInCompanion(): String = "K"
    }

    fun simple() {
        prop
        constVal
        funInCompanion()
    }

    fun asResult(): String = prop + constVal + funInCompanion() + c
}

fun box(): String {
    val r = Foo('2')
    if (r.asResult() != "O1K2") return "fail"
    return "OK"
}