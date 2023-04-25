// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR

var that: Int
    lateinit field: String
    get() = field.length
    set(value) {
        field = value.toString()
    }

fun box(): String {
    that = 1

    return if (that == 1) {
        "OK"
    } else {
        "fail: $that"
    }
}
