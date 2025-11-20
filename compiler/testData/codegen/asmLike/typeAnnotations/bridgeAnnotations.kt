// EMIT_JVM_TYPE_ANNOTATIONS
// JVM_DEFAULT_MODE: no-compatibility
// JVM_TARGET: 1.8
// RENDER_ANNOTATIONS
// LANGUAGE: +JvmEnhancedBridges

@Target(AnnotationTarget.PROPERTY)
annotation class PropertyAnno

@Target(AnnotationTarget.PROPERTY_GETTER)
annotation class PropertyGetterAnno

@Target(AnnotationTarget.FUNCTION)
annotation class Anno

@Target(AnnotationTarget.FUNCTION)
annotation class OldAnno

@Target(AnnotationTarget.TYPE)
annotation class TypeAnno

@Target(AnnotationTarget.TYPE)
annotation class OldTypeAnno

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class ParamAnno

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class OldParamAnno

interface I<T: Any> {
    @OldAnno
    fun foo(@OldParamAnno p1: @OldTypeAnno T, p2: T): @OldTypeAnno Any? = null
}

class C : I<Int> {
    @Anno
    override fun foo(a: @TypeAnno Int, @ParamAnno b: Int): @TypeAnno String = ""
}

abstract class MyCharSequence : CharSequence {
    @PropertyAnno // not applied to getter methods
    override val length: @TypeAnno Int
        @PropertyGetterAnno get() = 0

    @Anno
    override fun get(@ParamAnno index: @TypeAnno Int): @TypeAnno Char = throw Exception()
}
