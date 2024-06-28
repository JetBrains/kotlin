package test

inline fun <reified T> inlineFun(lambda: () -> String = { T::class.java.simpleName }): String {
    return lambda()
}

class OK

fun box(): String {
    return inlineFun<OK>()
}

// JVM_TEMPLATES
// 1 LOCALVARIABLE \$i\$a\$-inlineFun-DefaultLambdaKt\$inlineFun\$1 I
// inlineFun, inlineFun$default, inlined inlineFun:
// 3 LOCALVARIABLE \$i\$f\$inlineFun

// JVM_IR_TEMPLATES
// 1 LOCALVARIABLE \$i\$a\$-inlineFun-DefaultLambdaKt\$inlineFun\$1 I
// inlineFun, inlineFun$default, inlined inlineFun:
// 3 LOCALVARIABLE \$i\$f\$inlineFun

// JVM_IR_TEMPLATES_WITH_INLINE_SCOPES
// 1 LOCALVARIABLE \$i\$a\$-inlineFun-DefaultLambdaKt\$inlineFun\$1\\2\\30\\0 I
// inlineFun, inlineFun$default, inlined inlineFun:
// 1 LOCALVARIABLE \$i\$f\$inlineFun\\1
// 2 LOCALVARIABLE \$i\$f\$inlineFun I
