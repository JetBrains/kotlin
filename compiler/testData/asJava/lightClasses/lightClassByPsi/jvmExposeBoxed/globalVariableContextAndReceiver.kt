// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ContextParameters
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class A(val value: String)

@JvmInline
value class Z(val value: String)

@get:JvmExposeBoxed
@set:JvmExposeBoxed
context(_: Z)
var A.f: String
    get() = ""
    set(value) {

    }
