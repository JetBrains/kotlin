// LIBRARY_PLATFORMS: JVM, Common
// Array class literals carry the array nestedness in addition to the class id.
// They are not supported by the JS platform: "only classes are allowed on the left-hand side of a class literal"
// FILE: ClassLiteral.kt
import kotlin.reflect.KClass

annotation class ClassLiteral(
    val c1: KClass<*>,
    val c2: KClass<*>,
    val c3: KClass<*>,
)

// FILE: WithArrayClassLiterals.kt
@ClassLiteral(
    Array<Int>::class,
    Array<Array<String>>::class,
    Array<ClassLiteral>::class,
)
class WithArrayClassLiterals

// FILE: WithPrimitiveArrayClassLiterals.kt
@ClassLiteral(
    IntArray::class,
    Array<IntArray>::class,
    Array<Array<IntArray>>::class,
)
class WithPrimitiveArrayClassLiterals
