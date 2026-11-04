// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class Wrapper(val s: String)

typealias NullableUInt = UInt?

typealias AliasedUInt = UInt

fun plainNullable(vararg xs: UInt?): Int = xs.size

fun nullableNotLast(vararg xs: ULong?, s: String): Int = xs.size

fun aliasedNullable(vararg xs: NullableUInt): Int = xs.size

fun <T : UInt> boundedByUnsigned(vararg xs: T): Int = xs.size

fun <T : Wrapper> boundedByValueClass(vararg xs: T): Int = xs.size

fun mangled(vararg xs: UInt): Int = xs.size

fun aliasedMangled(vararg xs: AliasedUInt): Int = xs.size

class H(vararg val xs: UInt?) {
    fun member(vararg xs: UShort?): Int = xs.size

    fun <T : Wrapper> boundedMember(vararg xs: T): Int = xs.size
}

class Bounded<T : Wrapper>(vararg val xs: T)

class Mangled(vararg val xs: UInt)

@JvmExposeBoxed
fun exposedNullable(vararg xs: UInt?): Int = xs.size

@JvmExposeBoxed
class ExposedH(vararg val xs: UByte?) {
    fun member(vararg xs: UByte?): Int = xs.size
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: VarargManglingKt.class[AliasedUInt;NullableUInt;aliasedMangled;mangled]
// LIGHT_ELEMENTS_NO_DECLARATION: Mangled.class[getXs--hP7Qyg], VarargManglingKt.class[aliasedMangled--ajY-9A;mangled--ajY-9A], Wrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]
