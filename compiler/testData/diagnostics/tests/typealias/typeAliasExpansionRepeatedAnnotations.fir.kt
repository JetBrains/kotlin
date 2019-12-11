@Target(AnnotationTarget.TYPE)
annotation class A

typealias AInt = @A Int
typealias AI = AInt

typealias Test1 = @A AInt
typealias Test2 = @A AI
typealias Test3 = List<@A AInt>
typealias Test4 = List<@A AI>

val testProperty1: @A AInt = 0
val testProperty2: @A AI = 0

fun testFunction1(x: @A AInt): @A AInt = x
fun testFunction2(x: @A AI): @A AI = x