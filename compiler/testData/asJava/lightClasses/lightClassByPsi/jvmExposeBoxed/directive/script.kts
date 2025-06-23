// WITH_STDLIB
// LANGUAGE: +ImplicitJvmExposeBoxed

@JvmInline
value class StringWrapper(val s: String)

class Clazz {
    fun foo(s: StringWrapper): String = s.s
    val bar: StringWrapper get() = StringWrapper("OK")
}
