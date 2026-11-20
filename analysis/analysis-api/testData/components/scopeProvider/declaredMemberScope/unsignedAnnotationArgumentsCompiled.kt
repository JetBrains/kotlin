// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package test

const val MAX_UINT: UInt = UInt.MAX_VALUE

@Target(AnnotationTarget.TYPE)
annotation class AnnoWithDefaults(val b: UByte = 5u, val s: UShort = 6u, val i: UInt = 7u, val l: ULong = 8u)

@Target(AnnotationTarget.TYPE)
annotation class TypeAnno(val b: UByte, val s: UShort, val i: UInt, val l: ULong)

@Target(AnnotationTarget.TYPE)
annotation class NestedAnno(val single: TypeAnno, val array: Array<TypeAnno>)

interface MyInterface {
    val property: @TypeAnno(1u, 2u, 3u, 4u) String

    val propertyWithDefaults: @AnnoWithDefaults Int

    fun function(argument: @TypeAnno(255u, 65535u, 4294967295u, 18446744073709551615u) Int): @TypeAnno(1u, 2u, 3u, 4u) Int

    fun constants(): @TypeAnno(UByte.MAX_VALUE, UShort.MIN_VALUE, MAX_UINT, ULong.MAX_VALUE) Int

    fun nested(): @NestedAnno(TypeAnno(1u, 2u, 3u, 4u), [TypeAnno(255u, 65535u, 4294967295u, 18446744073709551615u)]) Int

    fun typeArgument(): List<@TypeAnno(255u, 65535u, 4294967295u, 18446744073709551615u) String>
}

// MODULE: main(lib)
// FILE: main.kt

// class: test/MyInterface
