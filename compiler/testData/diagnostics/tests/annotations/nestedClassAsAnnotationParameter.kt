// FIR_IDENTICAL
import kotlin.reflect.KClass

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.TYPE_PARAMETER,
    AnnotationTarget.TYPE,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CONSTRUCTOR
)
annotation class Special(val why: KClass<*>)

interface Interface

class Outer @Special(Nested::class) constructor(
    @Special(Nested::class)
    val why: KClass<*>
) {
    @Special(Nested::class)
    class Nested<@Special(Nested::class) T> : @Special(Nested::class) Interface

    @Special(Nested::class)
    val why2: KClass<*>? = null

    @Special(Nested::class)
    fun why3() {}
}

enum class E(
    @Special(Nested::class)
    val why: KClass<*>
) {
    @Special(Nested::class)
    Foo(Nested::class);

    class Nested
}