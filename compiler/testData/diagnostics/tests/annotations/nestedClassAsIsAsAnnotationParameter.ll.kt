// LL_FIR_DIVERGENCE
// KT-62587
// LL_FIR_DIVERGENCE
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.TYPE)
annotation class Special(val why: KClass<*>)

interface Interface

class Outer {
    @Special(<!UNRESOLVED_REFERENCE!>Nested<!>)
    class Nested<@Special(<!ARGUMENT_TYPE_MISMATCH, NO_COMPANION_OBJECT!>Nested<!>) T> : @Special(<!UNRESOLVED_REFERENCE!>Nested<!>) Interface
}
