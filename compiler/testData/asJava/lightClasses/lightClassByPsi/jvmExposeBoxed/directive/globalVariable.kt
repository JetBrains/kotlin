// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

@JvmInline
value class StringWrapper(val s: String)

var foo: StringWrapper
    get() = StringWrapper("str")
    set(value) {

    }
