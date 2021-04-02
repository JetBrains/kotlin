// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

fun interface GenericToAny<T> {
    fun invoke(x: T): Any
}

fun interface GenericIntToAny : GenericToAny<Int>

fun with4(fn: GenericIntToAny) = fn.invoke(4).toString()

fun box(): String =
    with4 {
        if (it != 4) throw Exception()
        "OK"
    }