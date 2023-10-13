import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.TYPE)
annotation class Special(val why: KClass<*>)

interface Interface

class Outer {
    @Special(<!NO_COMPANION_OBJECT!>Nested<!>)
    class Nested<@Special(<!NO_COMPANION_OBJECT!>Nested<!>) T> : @Special(<!NO_COMPANION_OBJECT!>Nested<!>) Interface
}
