// LL_FIR_DIVERGENCE
// KT-62587
// LL_FIR_DIVERGENCE
// FIR_IDENTICAL
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class Special(val why: KClass<*>)

interface Interface

object Outer {
    @Special(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>Nested<!>::class<!>)
    object Nested : @Special(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>Nested<!>::class<!>) Interface
}
