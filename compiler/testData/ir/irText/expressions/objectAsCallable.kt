// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

// KT-61141: absent enum fake_overrides: finalize, getDeclaringClass, clone
// IGNORE_BACKEND: NATIVE

object A

enum class En { X }

operator fun A.invoke(i: Int) = i
operator fun En.invoke(i: Int) = i

val test1 = A(42)
val test2 = En.X(42)
