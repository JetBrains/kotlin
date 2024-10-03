// LANGUAGE: +FunctionalTypeWithExtensionAsSupertype
// TARGET_BACKEND: JVM
// WITH_STDLIB
@JvmInline
value class ValueClass(private val s: Int) : Int.() -> String {
    override fun invoke(p1: Int): String {
        return "OK"
    }
}

fun box(): String {
    return ValueClass(1)(1)
}
