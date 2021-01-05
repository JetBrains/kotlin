// FILE: 1.kt
// Intentionally in the same package, as objects in other packages are always regenerated.

fun f(x: () -> String) = x()

inline fun g(crossinline x: () -> String) = f { x() }

// FILE: 2.kt
//   _1Kt$g$1 not regenerated because the original already does the same thing (invoking a functional object)
//                         \-v
fun h(x: () -> String) = g { g(x) }
//                     /-^
//   _1Kt$g$1 regenerated to inline the lambda

fun box() = h { "OK" }
