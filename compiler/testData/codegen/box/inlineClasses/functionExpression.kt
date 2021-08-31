inline fun new(init: (Z) -> Unit): Z = Z(42)

inline class Z(val value: Int)

fun box(): String =
    if (new(fun(z: Z) {}).value == 42) "OK" else "Fail"
