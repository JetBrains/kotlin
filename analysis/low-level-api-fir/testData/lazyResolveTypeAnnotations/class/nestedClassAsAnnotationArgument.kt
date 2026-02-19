import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.TYPE)
annotation class Special(val why: KClass<*>)

interface Interface

class Outer {
    @Special(Nested::class)
    class N<caret>ested<@Special(Nested::class) T> : @Special(Nested::class) Interface
}
