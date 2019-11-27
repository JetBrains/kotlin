// IGNORE_BACKEND_FIR: JVM_IR
var <T> T.varProp: T
    get() = this
    set(value: T) {}

class A {
    var int: Int = 0
    var long: Long = 0.toLong()
    var short: Short = 0.toShort()
    var byte: Byte = 0.toByte()
    var double: Double = 0.0
    var float: Float = 0.0f
    var char: Char = '0'
    var bool: Boolean = false

    operator fun invoke() {
        int.varProp = int
        long.varProp = long
        short.varProp = short
        byte.varProp = byte
        double.varProp = double
        float.varProp = float
        char.varProp = char
        bool.varProp = bool
    }
}

fun box(): String {
    0.varProp = 0
    false.varProp = false
    '0'.varProp = '0'
    0.0.varProp = 0.0
    0.0f.varProp = 0.0f
    0.toByte().varProp = 0.toByte()
    0.toShort().varProp = 0.toShort()
    0.toLong().varProp = 0.toLong()

    A()()

    return "OK"
}