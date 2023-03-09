// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

@file:A("File annotation")
package test

@Target(AnnotationTarget.FILE)
annotation class A(val x: String)
