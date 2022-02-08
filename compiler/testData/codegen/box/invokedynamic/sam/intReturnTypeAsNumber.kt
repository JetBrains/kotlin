// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory\.metafactory

fun interface INum {
    fun get(): Number
}

fun box(): String {
    val num = INum { 42 }
    if (num.get() != 42)
        return "Failed"
    return "OK"
}
