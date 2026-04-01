// EMIT_JVM_TYPE_ANNOTATIONS
// JVM_DEFAULT_MODE: no-compatibility
// JVM_TARGET: 1.8
// RENDER_ANNOTATIONS
// LANGUAGE: +JvmEnhancedBridges
// IGNORED_ANNOTATIONS_FOR_BRIDGES: main.IgnoredFunAnno, main.IgnoredTypeAnno, main.IgnoredParamAnno

// FILE: main.kt

package main

@Target(AnnotationTarget.FUNCTION)
annotation class FunAnno

@Target(AnnotationTarget.FUNCTION)
annotation class IgnoredFunAnno

@Target(AnnotationTarget.TYPE)
annotation class TypeAnno

@Target(AnnotationTarget.TYPE)
annotation class IgnoredTypeAnno

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class ParamAnno

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class IgnoredParamAnno

interface I<T: Any> {
    fun foo(p1: T, p2: T): Any? = null
}

class C : I<Int> {
    @FunAnno
    @IgnoredFunAnno
    override fun foo(a: @TypeAnno @IgnoredTypeAnno Int, @ParamAnno @IgnoredParamAnno b: Int): @TypeAnno @IgnoredTypeAnno String = ""
}
