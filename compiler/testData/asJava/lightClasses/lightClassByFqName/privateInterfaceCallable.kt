// MyInterface
// SKIP_IDE_TEST
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

@JvmInline
value class StringWrapper(val s: String)

interface MyInterface {
    fun publicRegularMethod() {}

    private fun regularMethod() {}

    fun publicMangledMethod(s: StringWrapper) {}

    private fun mangledMethod(s: StringWrapper) {}

    var publicRegularVariable: Int
        get() = 0
        set(value) {}

    private var regularVariable: Int
        get() = 0
        set(value) {}

    var StringWrapper.publicMangledVariable: String
        get() = ""
        set(value) {}

    private var StringWrapper.mangledVariable: String
        get() = ""
        set(value) {}
}
