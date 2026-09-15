// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class MyUInt(val value: UInt)

@JvmExposeBoxed
class MyClass(val value: MyUInt) {
    constructor(value: UInt, text: String) : this(MyUInt(value))
}

// LIGHT_ELEMENTS_NO_DECLARATION: MyClass.class[getValue-wUwyISk], MyUInt.class[constructor-impl;equals-impl;equals-impl0;getValue-pVg5ArA;hashCode-impl;toString-impl]
