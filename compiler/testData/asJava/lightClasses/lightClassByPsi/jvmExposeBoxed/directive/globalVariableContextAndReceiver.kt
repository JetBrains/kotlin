// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ContextParameters +ImplicitJvmExposeBoxed

@JvmInline
value class A(val value: String)

@JvmInline
value class Z(val value: String)

context(_: Z)
var A.f: String
    get() = ""
    set(value) {

    }
