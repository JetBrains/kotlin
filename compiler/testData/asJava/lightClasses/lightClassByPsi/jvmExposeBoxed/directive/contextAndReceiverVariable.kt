// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ContextParameters +ImplicitJvmExposeBoxed
@JvmInline
value class B(val value: String)

@JvmInline
value class Z(val value: String)

class A {
    context(_: Z)
    var B.f: String
        get() = ""
        set(value) {

        }
}
