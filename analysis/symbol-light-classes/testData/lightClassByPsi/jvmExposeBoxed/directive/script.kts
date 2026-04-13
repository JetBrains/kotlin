// WITH_STDLIB
// JVM_EXPOSE_BOXED

@JvmInline
value class StringWrapper(val s: String)

class Clazz {
    fun foo(s: StringWrapper): String = s.s
    val bar: StringWrapper get() = StringWrapper("OK")
}
