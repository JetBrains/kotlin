// EMIT_JVM_TYPE_ANNOTATIONS
// JVM_DEFAULT_MODE: no-compatibility
// JVM_TARGET: 1.8
// RENDER_ANNOTATIONS
// LANGUAGE: +JvmEnhancedBridges
// IGNORED_ANNOTATIONS_FOR_BRIDGES: *

// FILE: main.kt

package main

@Target(AnnotationTarget.FUNCTION)
annotation class FunAnno

@Target(AnnotationTarget.FUNCTION)
annotation class OldFunAnno

@Target(AnnotationTarget.TYPE)
annotation class TypeAnno

@Target(AnnotationTarget.TYPE)
annotation class OldTypeAnno

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class ParamAnno

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class OldParamAnno

interface I<T: Any> {
    @OldFunAnno
    fun foo(@OldParamAnno p1: @OldTypeAnno T): @OldTypeAnno Any? = null
}

class C : I<Int> {
    @FunAnno
    override fun foo(@ParamAnno p1: @TypeAnno Int): @TypeAnno String = ""
}
