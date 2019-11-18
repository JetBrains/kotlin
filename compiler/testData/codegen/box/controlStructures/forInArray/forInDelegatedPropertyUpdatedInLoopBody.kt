// IGNORE_BACKEND_FIR: JVM_IR
class Del<T>(var x: T) {
    operator fun getValue(thisRef: Any?, kProp: Any) = x

    operator fun setValue(thisRef: Any?, kProp: Any, value: T) {
        x = value
    }
}

fun box(): String {
    var xs by Del(intArrayOf(1, 2, 3))
    var sum = 0
    for (x in xs) {
        sum = sum * 10 + x
        xs = intArrayOf(4, 5, 6)
    }
    return if (sum == 123) "OK" else "Fail: $sum"
}