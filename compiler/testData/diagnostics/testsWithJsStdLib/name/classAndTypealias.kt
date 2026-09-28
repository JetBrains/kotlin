// RUN_PIPELINE_TILL: CODEGEN
package foo

@JsName("B") class A(val x: Int)

typealias B = A
