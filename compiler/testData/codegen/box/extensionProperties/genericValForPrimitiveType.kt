// IGNORE_BACKEND_FIR: JVM_IR
val <T> T.valProp: T
    get() = this

class A {
    val int: Int = 0
    val long: Long = 0.toLong()
    val short: Short = 0.toShort()
    val byte: Byte = 0.toByte()
    val double: Double = 0.0
    val float: Float = 0.0f
    val char: Char = '0'
    val bool: Boolean = false

    operator fun invoke() {
        int.valProp
        long.valProp
        short.valProp
        byte.valProp
        double.valProp
        float.valProp
        char.valProp
        bool.valProp
    }
}

fun box(): String {
    0.valProp
    false.valProp
    '0'.valProp
    0.0.valProp
    0.0f.valProp
    0.toByte().valProp
    0.toShort().valProp
    0.toLong().valProp

    A()()

    return "OK"
}