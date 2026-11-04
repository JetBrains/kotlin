// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)

@JvmInline
value class IC @JvmExposeBoxed constructor(val s: String) {
    @JvmExposeBoxed
    constructor(vararg xs: UInt) : this("O" + xs.sum())

    @JvmExposeBoxed
    fun member(vararg xs: UInt): String = s + xs.sum()
}

@JvmExposeBoxed
class UIntStack(vararg val items: UInt) {
    fun sum(vararg xs: UInt): UInt = items.sum() + xs.sum()
}

@JvmExposeBoxed
class Prefixed(val prefix: IC, vararg val items: UByte) {
    fun result(): String = prefix.s + items.sum()
}

open class Base {
    open fun f(vararg xs: UInt): String = "B" + xs.sum()
}

class Derived : Base() {
    @JvmExposeBoxed
    override fun f(vararg xs: UInt): String = "D" + xs.sum()
}

@JvmExposeBoxed
fun notLast(vararg xs: UInt, s: String): String = s + xs.sum()

@JvmExposeBoxed
fun sumULong(vararg xs: ULong): ULong = xs.sum()

@JvmExposeBoxed
fun sumUShort(prefix: String, vararg xs: UShort): String = prefix + xs.sum()

@JvmExposeBoxed("sumAll")
fun sumNamed(vararg xs: UInt): UInt = xs.sum()

@JvmExposeBoxed
@JvmOverloads
fun withDefaults(s: String = "", vararg xs: UInt = uintArrayOf()): String = s + xs.sum()

// DECLARATIONS_NO_LIGHT_ELEMENTS: Base.class[f], UnsignedVarargKt.class[sumNamed]
// LIGHT_ELEMENTS_NO_DECLARATION: Base.class[f--ajY-9A], Derived.class[f--ajY-9A], IC.class[constructor-impl;constructor-impl;equals-impl;equals-impl0;hashCode-impl;member--ajY-9A;toString-impl], Prefixed.class[getItems-TcUX1vc;getPrefix-fhib4bs], UIntStack.class[getItems--hP7Qyg;sum-JpblYG8], UnsignedVarargKt.class[notLast-LpG4sQ0;sumAll;sumAll;sumNamed--ajY-9A;sumULong-QwZRm1k;sumUShort-jDvGgag;withDefaults-wZx4R44]
