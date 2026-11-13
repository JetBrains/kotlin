// DUMP_IR_DIFFERENCE: JKLIB
// ^ JKlib tests compile against JDK 17 instead of the mock JDK.
enum class En { A, B, C, D }

annotation class TestAnn(val x: En)

@TestAnn(En.A)
fun test1() {}
