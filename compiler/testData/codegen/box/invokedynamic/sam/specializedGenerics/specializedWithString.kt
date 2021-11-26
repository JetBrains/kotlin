// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

fun interface GenericToAny<T> {
    fun invoke(Inner: T): T
}

fun <T> foo2(t: T, g: GenericToAny<T>): T = g.invoke(t)

fun box(): String {
    return foo2("OK") { it }
}