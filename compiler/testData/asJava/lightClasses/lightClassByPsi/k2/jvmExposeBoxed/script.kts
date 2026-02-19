// WITH_STDLIB

@JvmInline
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
class Clazz {
    @JvmExposeBoxed
    fun foo(s: StringWrapper): String = s.s

    @JvmExposeBoxed
    val bar: StringWrapper get() = StringWrapper("OK")
}
