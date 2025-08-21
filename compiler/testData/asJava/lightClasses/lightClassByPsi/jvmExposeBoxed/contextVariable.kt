// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ContextParameters
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class Z(val value: String)

@JvmExposeBoxed
class A {
    context(_: Z)
    var f: String
        get() = ""
        set(value) {

        }
}
