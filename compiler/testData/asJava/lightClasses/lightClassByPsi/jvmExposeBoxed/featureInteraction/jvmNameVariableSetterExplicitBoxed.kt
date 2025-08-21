// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class StringWrapper(val s: String)

class Foo {
    @set:JvmExposeBoxed
    @set:JvmName("setter")
    var foo: StringWrapper get() = StringWrapper("OK")
        set(value) {

        }
}
