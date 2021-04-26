// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

fun interface GenericToAny<T> {
    fun invoke(x: T): Any
}

fun interface GenericStringToAny : GenericToAny<String>

fun withK(fn: GenericStringToAny) = fn.invoke("K").toString()

fun box(): String =
    withK { "O" + it }