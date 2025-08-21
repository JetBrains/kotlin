// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class Special(val why: KClass<*>)

interface Interface

object Outer {
    @Special(Nested::class)
    object Nested : @Special(Nested::class) Interface
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classReference, interfaceDeclaration, nestedClass, objectDeclaration,
primaryConstructor, propertyDeclaration, starProjection */
