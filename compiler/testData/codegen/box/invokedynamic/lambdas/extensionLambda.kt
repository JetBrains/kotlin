// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// LAMBDAS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

class C(val x: String)

fun boxLambda(lambda: C.() -> String) = lambda

fun box(): String {
    val ext = boxLambda { x }
    return C("OK").ext()
}