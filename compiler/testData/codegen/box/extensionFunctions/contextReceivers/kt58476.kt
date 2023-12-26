// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// WITH_COROUTINES
// JVM_ABI_K1_K2_DIFF: different order of function annotations

class InContext
class MyReciever {
    public suspend fun MyOutput.innerFun(): Int = 123
}
class MyOutput

// 2 - Declare the caller that calls the suspended function in a context
public fun caller(block: suspend context(InContext) MyReciever.() -> Int): MyOutput = MyOutput()

fun box(): String {
    val out1 = caller {  MyOutput().innerFun() }
    val out2 = with (InContext()) { caller {  MyOutput().innerFun() } }
    val out3 = caller { with (InContext()) { MyOutput().innerFun() } }
    return "OK"
}