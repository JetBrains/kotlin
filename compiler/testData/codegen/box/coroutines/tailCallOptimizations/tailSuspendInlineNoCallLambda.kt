// TARGET_BACKEND: JVM

suspend inline fun lambdaAsParameterInline(c: suspend ()->Unit) { c() }
suspend inline fun lambdaAsParameterInline2(c: suspend ()->Unit) { lambdaAsParameterInline(c) }
suspend fun useLambdaAsParameterInline2NoCall() { lambdaAsParameterInline2 {  } }

fun box() = "OK"

// With the current implementation, state machine is expected only in `lambdaAsParameterInline2()`
// It is important not to have it in `useLambdaAsParameterInline2NoCall()`

// CHECK_BYTECODE_TEXT
// 1 TABLESWITCH