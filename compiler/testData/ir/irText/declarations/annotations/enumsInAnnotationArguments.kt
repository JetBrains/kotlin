// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

enum class En { A, B, C, D }

annotation class TestAnn(val x: En)

@TestAnn(En.A)
fun test1() {}
