// IGNORE_BACKEND_FIR: JVM_IR
class A<T>(var t: T) {}
class B<R>(val r: R) {}

fun box() : String {
    val ai = A<Int>(1)
    val aai = A<A<Int>>(ai)
    if(aai.t.t != 1)  return "fail"
/*
    aai.t.t = 2
    if(aai.t.t != 2)  return "fail"

    if(ai.t != 2)  return "fail"
    if(aai.t != ai)  return "fail"
    if(aai.t !== ai) return "fail"

    val abi = A<B<Int>>(B<Int>(1))
    if(abi.t.r != 1) return "fail"
*/
    return "OK"
}
