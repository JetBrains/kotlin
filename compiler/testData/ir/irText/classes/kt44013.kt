// TARGET_BACKEND: JVM
// SKIP_KT_DUMP
// IGNORE_FIR_DIAGNOSTICS

// See KT-44013: FE accepts incorrect code in 1.5

abstract class Test1 : () -> Int()

abstract class Base
abstract class Test2 : () -> Int(), Base()
