// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class StringWrapper(val s: String)

@JvmExposeBoxed
class Foo {
    @get:JvmName("foo11")
    val foo1: StringWrapper get() = StringWrapper("")

    @get:JvmName("foo41")
    val Int.foo4: StringWrapper get() = StringWrapper("")

    @get:JvmExposeBoxed("fooGetter22")
    @get:JvmName("fooGetter21")
    @set:JvmName("fooSetter21")
    var foo2: StringWrapper get() = StringWrapper("OK")
        set(value) {

        }

    @get:JvmExposeBoxed("fooGetter32")
    @get:JvmName("fooGetter31")
    @set:JvmName("fooSetter31")
    var Int.foo3: StringWrapper get() = StringWrapper("OK")
        set(value) {

        }
}
