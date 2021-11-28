// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 java/lang/invoke/LambdaMetafactory
// 1 final synthetic class BuiltinMemberReferenceKt\$box\$test\$1

fun interface IntFun {
    fun invoke(i: Int): Int
}

fun invoke1(intFun: IntFun) = intFun.invoke(1)

fun box(): String {
    val test = invoke1(41::plus)
    if (test != 42) return "Failed: $test"

    return "OK"
}