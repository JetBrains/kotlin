// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

// A user value class has no corresponding array type, so the only vararg *of* a value class is an unsigned
// one: 'vararg counts: UInt' has parameter type 'UIntArray', which is itself a '@JvmInline value class'. The
// declaration is therefore mangled and gets a boxed variant, unlike the 'vararg' cases in 'vararg.kt' and
// 'directive/varargAndValueClass.kt'.
@OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)
@JvmExposeBoxed
fun sumOf(vararg counts: UInt): UInt = counts.size.toUInt()

@OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)
class Host {
    @JvmExposeBoxed
    fun member(vararg counts: UInt): UInt = counts.size.toUInt()
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: Host.class[member], VarargUnsignedKt.class[sumOf]
