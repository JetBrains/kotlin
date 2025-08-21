// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.TYPE)
annotation class Special(val why: KClass<*>)

interface Interface

class Outer {
    @Special(Outer.Nested::class)
    class Nested<@Special(Outer.Nested::class) T> : @Special(Outer.Nested::class) Interface
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, classReference, interfaceDeclaration, nestedClass,
nullableType, primaryConstructor, propertyDeclaration, starProjection, typeParameter */
