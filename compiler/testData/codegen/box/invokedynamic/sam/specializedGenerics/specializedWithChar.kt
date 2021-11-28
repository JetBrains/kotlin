// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

fun interface GenericToAny<T> {
    fun invoke(Inner: T): Any
}

fun <T> foo2(t: T, g: GenericToAny<T>): Any = g.invoke(t)

fun box(): String {
    foo2<Char>('.') {  }
    return "OK"
}
