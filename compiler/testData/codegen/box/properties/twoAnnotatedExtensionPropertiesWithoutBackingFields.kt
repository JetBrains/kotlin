// IGNORE_BACKEND_FIR: JVM_IR
annotation class Anno

@Anno val Int.foo: Int
    get() = this

@Anno val String.foo: Int
    get() = 42

fun box() = if (42.foo == 42 && "OK".foo == 42) "OK" else "Fail"
