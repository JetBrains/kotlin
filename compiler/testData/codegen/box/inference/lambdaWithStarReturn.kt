fun invoke(f: Function1<Any?, *>) = f("OK")

fun box() = invoke { it } as String
