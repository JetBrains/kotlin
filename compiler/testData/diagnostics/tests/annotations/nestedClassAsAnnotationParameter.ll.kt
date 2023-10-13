// LL_FIR_DIVERGENCE
// KT-62587
// LL_FIR_DIVERGENCE
// FIR_IDENTICAL
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.TYPE)
annotation class Special(val why: KClass<*>)

interface Interface

class Outer {
    @Special(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>Nested<!>::class<!>)
    class Nested<@Special(Nested::class) T> : @Special(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>Nested<!>::class<!>) Interface
}
