class A

operator fun A.contains(i: A, actual: Boolean = true) = actual

fun box() = if (A() in A()) "OK" else "Fail"
