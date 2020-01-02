import kotlin.reflect.KClass

const val CONST = 1
fun foo() = 1
val nonConst = foo()

annotation class ValidAnn(
    val p1: Int = 1 + CONST,
    val p2: String = "",
    val p3: KClass<*> = String::class,
    val p4: IntArray = intArrayOf(1, 2, 3),
    val p5: Array<String> = arrayOf("abc"),
    val p6: Array<KClass<*>> = arrayOf(Int::class)
)

val nonConstKClass = String::class

annotation class InvalidAnn(
    val p1: Int = foo(),
    val p2: Int = nonConst,
    val p3: KClass<*> = nonConstKClass
)
