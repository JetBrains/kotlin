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

// LIGHT_ELEMENTS_NO_DECLARATION: A.class[getF-vo2-FlA;setF-75nD7FQ], B.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], Z.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]