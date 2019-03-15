// WITH_RUNTIME

package test

@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
annotation class Ann(
    val ubyte: UByte,
    val ushort: UShort,
    val uint: UInt,
    val ulong: ULong
)

const val ubyteConst: UByte = 10u
const val ushortConst: UShort = 20u
const val uintConst = 30u
const val ulongConst = 40uL

class A {
    fun unsigned(s: @Ann(1u, 2u, 3u, 4u) String) {}
    fun <@Ann(0xFFu, 0xFFFFu, 0xFFFF_FFFFu, 0xFFFF_FFFF_FFFF_FFFFuL) T> typeParam() {}
    fun unsignedConsts(s: @Ann(ubyteConst, ushortConst, uintConst, ulongConst) String) {}
}
