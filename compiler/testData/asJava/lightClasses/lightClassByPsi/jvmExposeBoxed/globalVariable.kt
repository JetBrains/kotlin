// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
@get:JvmExposeBoxed("getter")
@set:JvmExposeBoxed("setter")
var foo: StringWrapper
    get() = StringWrapper("str")
    set(value) {

    }
