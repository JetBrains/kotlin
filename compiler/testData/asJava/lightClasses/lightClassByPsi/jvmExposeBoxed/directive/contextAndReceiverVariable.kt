// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ContextParameters +ImplicitJvmExposeBoxed
@JvmInline
value class B(val value: String)

@JvmInline
value class Z(val value: String)

class A {
    context(_: Z, _: Boolean)
    var B.f: Int
        get() = 1
        set(value) {

        }
}
