// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: needs fix
// !LANGUAGE:+AssignOperatorOverload


class Materialize()
fun <T> materialize(): T = TODO()
operator fun Materialize.assign(v: String): Unit {
}

data class CallableReceiver(var result: String)
operator fun CallableReceiver.assign(r: CallableReceiver.() -> Unit): Unit {
    r.invoke(this)
}

fun box(): String {
    // Test materialize
    try {
        val materialize = Materialize()
        materialize = materialize()
        return "Fail"
    } catch (err: kotlin.NotImplementedError) {
        // OK
    }

    // Test callable receiver
    val receiver = CallableReceiver(result = "fail")
    receiver = { receiver.result = "OK.CallableReceiver" }
    if (receiver.result != "OK.CallableReceiver") return "Fail: ${receiver.result}"

    return "OK"
}