// DUMP_IR_DIFFERENCE: JKLIB
// ^ JKlib tests compile against JDK 17 instead of the mock JDK.
enum class Enum { A }
object A
val a = 0
class Z {
    companion object
}

fun test1() = Enum.A
fun test2() = A
fun test3() = a
fun test4() = Z
