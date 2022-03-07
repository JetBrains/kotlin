package test

inline fun <reified T> inlineFun(lambda: () -> String = { T::class.java.simpleName }): String {
    return lambda()
}

class OK

fun box(): String {
    return inlineFun<OK>()
}

// 1 LOCALVARIABLE \$i\$a\$-inlineFun-DefaultLambdaKt\$inlineFun\$1 I
// inlineFun, inlineFun$default, inlined inlineFun:
// 3 LOCALVARIABLE \$i\$f\$inlineFun