// IGNORE_BACKEND_FIR: JVM_IR
open class T(var value: Int) {}

fun localExtensionOnNullableParameter(): T {

    fun T.local(s: Int) {
        value += s
    }

    var t: T? = T(1)
    t?.local(2)

    return t!!
}


fun box(): String {
    val result = localExtensionOnNullableParameter().value
    if (result != 3) return "fail 2: $result"

    return "OK"
}
