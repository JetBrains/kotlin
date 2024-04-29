// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: CLASS
// JVM_ABI_K1_K2_DIFF: Caused by empty lambda handling, will be fixed in the later commit

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 java/lang/invoke/LambdaMetafactory

fun interface GenericToAny<T> {
    fun invoke(Inner: T): Any
}

fun <T> foo2(t: T, g: GenericToAny<T>): Any = g.invoke(t)

fun box(): String {
    foo2<Char>('.') {  }
    return "OK"
}
