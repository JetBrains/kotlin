// DUMP_IR_DIFFERENCE: JKLIB
// ^ JKlib tests compile against JDK 17 instead of the mock JDK.
object A

enum class En { X }

operator fun A.invoke(i: Int) = i
operator fun En.invoke(i: Int) = i

val test1 = A(42)
val test2 = En.X(42)
