// FILE: ClassLiteral.kt
import kotlin.reflect.KClass

annotation class ClassLiteral(
    val c1: KClass<*>,
    val c2: KClass<*>,
)

// FILE: WithClassLiteral.kt
@ClassLiteral(
    WithClassLiteral::class,
    Boolean::class,
)
class WithClassLiteral
