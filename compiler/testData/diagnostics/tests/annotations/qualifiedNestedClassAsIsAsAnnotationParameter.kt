import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.TYPE)
annotation class Special(val why: KClass<*>)

interface Interface

class Outer {
    @Special(Outer.<!NO_COMPANION_OBJECT!>Nested<!>)
    class Nested<@Special(Outer.<!NO_COMPANION_OBJECT!>Nested<!>) T> : @Special(Outer.<!NO_COMPANION_OBJECT!>Nested<!>) Interface
}
