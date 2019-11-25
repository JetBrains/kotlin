// IGNORE_BACKEND_FIR: JVM_IR
typealias BoolArray = Array<Boolean>
typealias IArray = IntArray
typealias MyArray<T> = Array<T>

fun box(): String {
    val ba = BoolArray(1) { true }
    if (!ba[0]) return "Fail #1"

    val ia = IArray(1) { 42 }
    if (ia[0] != 42) return "Fail #2"

    val ma = MyArray<Int>(1) { 42 }
    if (ma[0] != 42) return "Fail #2"

    return "OK"
}