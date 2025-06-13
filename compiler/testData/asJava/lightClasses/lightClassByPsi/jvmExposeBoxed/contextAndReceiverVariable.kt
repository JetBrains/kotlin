// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ContextParameters
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class B(val value: String)

@JvmInline
value class Z(val value: String)

@JvmExposeBoxed
class A {
    context(_: Z)
    var B.f: String
        get() = ""
        set(value) {

        }
}
