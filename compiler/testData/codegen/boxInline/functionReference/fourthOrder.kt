// KT-72884: Internal error in body lowering: IllegalStateException: Can't inline given reference, it should've been lowered
// IGNORE_BACKEND: NATIVE, JS_IR, JS_IR_ES6, WASM
// IGNORE_INLINER: IR
// ^^^ ignores JVM_IR with JVM IR Inliner, in runner IrBlackBoxCodegenWithIrInlinerTestGenerated

// FILE: 1.kt
typealias FirstOrder = () -> String
typealias SecondOrder = (FirstOrder) -> String
typealias ThirdOrder = (SecondOrder) -> String

inline fun firstImpl(): String = "OK"
inline fun secondImpl(crossinline first: FirstOrder): String = first()
inline fun thirdImpl(crossinline second: SecondOrder): String = second(::firstImpl)
inline fun fourthImpl(crossinline third: ThirdOrder): String = third(::secondImpl)

// FILE: 2.kt
fun box() = fourthImpl(::thirdImpl)
