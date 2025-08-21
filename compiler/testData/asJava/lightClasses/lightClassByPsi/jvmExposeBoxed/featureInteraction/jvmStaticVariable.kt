// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
object Foo {
    @get:JvmExposeBoxed
    @set:JvmExposeBoxed
    @JvmStatic
    var baz: StringWrapper get() = StringWrapper("OK")
        set(value) {

        }
}
