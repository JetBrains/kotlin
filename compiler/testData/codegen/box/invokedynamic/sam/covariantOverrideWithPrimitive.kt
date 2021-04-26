// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 java/lang/invoke/LambdaMetafactory
// 1 final class CovariantOverrideWithPrimitiveKt\$box\$test\$

fun interface IFooAny {
    fun foo(): Any
}

fun interface IFooInt : IFooAny {
    override fun foo(): Int
}

fun box(): String {
    val test = IFooInt { 42 }
    if (test.foo() != 42)
        return "Failed"

    return "OK"
}