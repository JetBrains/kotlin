// LANGUAGE: +ValhallaValueClasses
// IGNORE_BACKEND_K1: ANY
// RUN_PIPELINE_TILL: FRONTEND
// JVM_TARGET: 23
// EXPLICIT_API_MODE: STRICT
// ENABLE_JVM_PREVIEW

public value class FooValue(val i: Int)
<!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>value class FooValue2<!>(val i: Int)
