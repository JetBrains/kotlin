data class A(val x: String, val y: String)

suspend inline fun foo(a: A, block: suspend (A) -> String): String = block(a)

// FILE: test.kt
suspend fun test() = foo(A("O", "K")) { (x_param, y_param) -> x_param + y_param }

// JVM_TEMPLATES
// @TestKt.class:
// 1 LOCALVARIABLE x_param Ljava/lang/String;
// 1 LOCALVARIABLE y_param Ljava/lang/String;

// JVM_IR_TEMPLATES
// @TestKt.class:
// 1 LOCALVARIABLE x_param Ljava/lang/String;
// 1 LOCALVARIABLE y_param Ljava/lang/String;

// JVM_IR_TEMPLATES_WITH_INLINE_SCOPES
// 1 LOCALVARIABLE x_param\\2 Ljava/lang/String;
// 1 LOCALVARIABLE y_param\\2 Ljava/lang/String;
