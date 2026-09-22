// RUN_PIPELINE_TILL: FRONTEND
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.TYPE)
annotation class Special(val why: KClass<*>)

interface Interface

class Outer {
    @Special(<!UNRESOLVED_REFERENCE!>NestedNested<!>::class)
    class Nested<@Special(<!UNRESOLVED_REFERENCE!>NestedNested<!>::class) T> : @Special(<!UNRESOLVED_REFERENCE!>NestedNested<!>::class) Interface {
        class NestedNested
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, classReference, interfaceDeclaration, nestedClass,
nullableType, primaryConstructor, propertyDeclaration, starProjection, typeParameter */
