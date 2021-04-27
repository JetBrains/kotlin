// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

class Scope

fun <T> simpleAsync0(block: Scope.() -> T) {}
fun <T> simpleAsync1(block: suspend Scope.() -> T) {}
suspend fun <T> simpleAsync2(block: Scope.() -> T) {}
suspend fun <T> simpleAsync3(block: suspend Scope.() -> T) {}

fun insideJob0() = doTheJob0()
fun insideJob1() = doTheJob1()
suspend fun insideJob2() = doTheJob2()
suspend fun insideJob3() = doTheJob3()

fun doTheJob0() = simpleAsync0 { <!ARGUMENT_TYPE_MISMATCH!>insideJob0()<!> }
fun doTheJob1() = simpleAsync1 { <!ARGUMENT_TYPE_MISMATCH!>insideJob1()<!> }
suspend fun doTheJob2() = simpleAsync2 { <!ARGUMENT_TYPE_MISMATCH!>insideJob2()<!> }
suspend fun doTheJob3() = simpleAsync3 { <!ARGUMENT_TYPE_MISMATCH!>insideJob3()<!> }
