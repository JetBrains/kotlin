package test

inline fun <reified T> inlineFun(lambda: () -> String = { T::class.java.simpleName }): String {
    return lambda()
}

class OK

fun box(): String {
    return inlineFun<OK>()
}

// 1 LOCALVARIABLE \$i\$a\$-inlineFun-DefaultLambdaKt\$inlineFun\$1\\2\\0 I
// inlineFun, inlineFun$default, inlined inlineFun:
// 1 LOCALVARIABLE \$i\$f\$inlineFun\\1
// 2 LOCALVARIABLE \$i\$f\$inlineFun I
