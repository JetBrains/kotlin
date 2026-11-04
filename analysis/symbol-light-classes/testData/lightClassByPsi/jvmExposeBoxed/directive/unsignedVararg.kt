// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// JVM_EXPOSE_BOXED
@file:OptIn(ExperimentalUnsignedTypes::class)

fun sum(vararg xs: UInt): UInt = xs.sum()

fun notLast(vararg xs: ULong, s: String): Int = xs.size

class Stack(vararg val items: UInt) {
    fun push(vararg xs: UByte) {}
}

open class Open {
    open fun overridable(vararg xs: UInt) {}

    fun notOverridable(vararg xs: UShort) {}
}

@JvmInline
value class Wrapper(val s: String) {
    constructor(vararg xs: UInt) : this(xs.joinToString())

    fun member(vararg xs: UInt) {}
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: Open.class[overridable]
// LIGHT_ELEMENTS_NO_DECLARATION: Open.class[notOverridable-rL5Bavg;overridable--ajY-9A], Stack.class[getItems--hP7Qyg;push-GBYM_sE], UnsignedVarargKt.class[notLast-tBf0fek;sum--ajY-9A], Wrapper.class[constructor-impl;constructor-impl;equals-impl;equals-impl0;hashCode-impl;member--ajY-9A;toString-impl]
