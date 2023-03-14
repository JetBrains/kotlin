// FIR_IDENTICAL
// WITH_STDLIB
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

annotation class Ann

@delegate:Ann
val test1 by lazy { 42 }
