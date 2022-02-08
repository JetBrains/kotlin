// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// LAMBDAS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 java/lang/invoke/LambdaMetafactory
// 1 class LambdaWithInlineAnyKt\$box\$t\$1

inline class Z(val value: Any)

fun foo1(fs: (Z) -> Z) = fs(Z(1))

fun box(): String {
    val t = foo1 { Z((it.value as Int) + 41) }
    if (t.value != 42) return "Failed: t=$t"

    return "OK"
}
