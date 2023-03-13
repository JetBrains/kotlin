// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

object A

enum class En { X }

operator fun A.invoke(i: Int) = i
operator fun En.invoke(i: Int) = i

val test1 = A(42)
val test2 = En.X(42)
