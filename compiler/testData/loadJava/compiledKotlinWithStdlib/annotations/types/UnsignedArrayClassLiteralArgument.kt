// ALLOW_AST_ACCESS
// WITH_STDLIB
// OPT_IN: kotlin.ExperimentalUnsignedTypes
// ISSUE: KT-91728

package test

import kotlin.reflect.KClass

@Target(AnnotationTarget.TYPE)
annotation class Ann(val klass: KClass<*>)

class Usage {
    fun unsignedArrays(
        a: @Ann(UByteArray::class) String,
        b: @Ann(UShortArray::class) String,
        c: @Ann(UIntArray::class) String,
        d: @Ann(ULongArray::class) String,
    ) {}

    fun primitiveArray(a: @Ann(IntArray::class) String) {}
}
