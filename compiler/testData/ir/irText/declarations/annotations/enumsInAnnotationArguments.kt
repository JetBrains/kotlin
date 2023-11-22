// IGNORE_BACKEND: JS_IR

// KT-61141: absent enum fake_overrides: finalize, getDeclaringClass, clone
// IGNORE_BACKEND: NATIVE

enum class En { A, B, C, D }

annotation class TestAnn(val x: En)

@TestAnn(En.A)
fun test1() {}
