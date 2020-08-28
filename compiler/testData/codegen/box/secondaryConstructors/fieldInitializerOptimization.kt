// IGNORE_BACKEND: JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

open class Base {
    open fun setup() {}
    init { setup() }
}

val placeHolder = Any()

class Derived : Base {
    constructor() : super()
    override fun setup() {
        xBool = true
        xByte = 1.toByte()
        xChar = 2.toChar()
        xShort = 3.toShort()
        xInt = 4
        xLong = 5L
        xFloat = 6.0f
        xDouble = 7.0
        xRef = placeHolder
    }

    // Technically, this field initializer comes after the superclass
    // constructor is called. However, we optimize away field initializers
    // which set fields to their default value, which is why x ends up with
    // value 1 after the constructor call.
    var xBool = false
    var xByte = 0.toByte()
    var xChar = 0.toChar()
    var xShort = 0.toShort()
    var xInt = 0
    var xLong = 0L
    var xFloat = 0.0f
    var xDouble = 0.0
    var xRef: Any? = null
}

fun box(): String {
    val d = Derived()
    if (d.xBool != true) return "fail Bool"
    if (d.xByte != 1.toByte()) return "fail Byte"
    if (d.xChar != 2.toChar()) return "fail Char"
    if (d.xShort != 3.toShort()) return "fail Short"
    if (d.xInt != 4) return "fail Int"
    if (d.xLong != 5L) return "fail Long"
    if (d.xFloat != 6.0f) return "fail Float"
    if (d.xDouble != 7.0) return "fail Double"
    if (d.xRef != placeHolder) return "fail Ref"
    return "OK"
}
