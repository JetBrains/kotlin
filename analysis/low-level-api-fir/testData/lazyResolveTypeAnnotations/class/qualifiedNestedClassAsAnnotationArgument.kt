import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.TYPE)
annotation class Special(val why: KClass<*>)

interface Interface

class Outer {
    @Special(Outer.Nested::class)
    class N<caret>ested<@Special(Outer.Nested::class) T> : @Special(Outer.Nested::class) Interface
}
