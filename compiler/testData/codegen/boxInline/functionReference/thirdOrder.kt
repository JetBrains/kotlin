// KT-72884: Internal error in body lowering: IllegalStateException: Can't inline given reference, it should've been lowered
// IGNORE_BACKEND: NATIVE, JS_IR, JS_IR_ES6, WASM
// IGNORE_INLINER: IR
// ^^^ ignores JVM_IR with JVM IR Inliner, in runner IrBlackBoxCodegenWithIrInlinerTestGenerated

// FILE: 1.kt
typealias FirstOrder = () -> String
typealias SecondOrder = (FirstOrder) -> String

fun firstImpl() = "OK"
fun secondImpl(first: FirstOrder) = first()
inline fun thirdImpl(crossinline second: SecondOrder) = second(::firstImpl)

// FILE: 2.kt
fun box() = thirdImpl(::secondImpl)
