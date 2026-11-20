// TARGET_BACKEND: JVM
// ALLOW_AST_ACCESS
// WITH_STDLIB
// OPT_IN: kotlin.ExperimentalUnsignedTypes
// ISSUE: KT-91728, KT-91727

package test

import kotlin.reflect.KClass

@Target(AnnotationTarget.TYPE)
annotation class Ann(val klass: KClass<*>)

class Usage {
    fun arraysOfUnsignedArrays(
        a: @Ann(Array<UIntArray>::class) String,
        b: @Ann(Array<Array<UIntArray>>::class) String,
    ) {}

    fun arrayOfUnsigned(a: @Ann(Array<UInt>::class) String) {}

    fun arraysOfPrimitives(
        a: @Ann(Array<IntArray>::class) String,
        b: @Ann(Array<Int>::class) String,
    ) {}
}
