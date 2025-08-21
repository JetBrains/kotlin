// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed
@JvmInline
value class Z(val value: String)

class A {
    var Z.f: String
        get() = ""
        set(value) {

        }
}
