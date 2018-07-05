// WITH_UNSIGNED

package test

@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
annotation class Ann(
    val ubyte: UByte,
    val ushort: UShort,
    val uint: UInt,
    val ulong: ULong
)

class A {
    fun unsigned(s: @Ann(1u, 2u, 3u, 4u) String) {}
    fun <@Ann(0xFFu, 0xFFFFu, 0xFFFF_FFFFu, 0xFFFF_FFFF_FFFF_FFFFuL) T> typeParam() {}
}
