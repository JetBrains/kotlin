// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

fun interface IFooAny {
    fun foo(x: Any): Any
}

fun interface IFooStr : IFooAny {
    override fun foo(x: Any): String
}

fun box() = IFooStr { x: Any -> x.toString() }.foo("OK")