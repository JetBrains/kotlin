// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6, NATIVE, WASM, JVM_OLD
// KOTLIN_SCRIPT_DEFINITION: org.jetbrains.kotlin.codegen.TestScriptWithReceivers

// receiver: abracadabra
// expected: rv=cadabra

// KT-55068

class User(var property: String = drop(4))

val rv = User().property
