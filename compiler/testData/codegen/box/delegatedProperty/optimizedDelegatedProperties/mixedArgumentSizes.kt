// IGNORE_BACKEND_K1: WASM

inline operator fun Double.provideDelegate(thisRef: Any?, kProp: Any?) = this.toLong()

inline operator fun Long.getValue(thisRef: Any?, kProp: Any?) = this.toInt()

inline operator fun Long.getValue(thisRef: Long, kProp: Any?) = this.toInt() + thisRef.toInt()

inline operator fun Long.getValue(thisRef: IntArray, kProp: Any?) = thisRef[this.toInt()]

inline operator fun Long.setValue(thisRef: IntArray, kProp: Any?, newValue: Int) {
    thisRef[this.toInt()] = newValue
}

val magic1 by 42.0

val magic2 by 42L

var IntArray.firstElement by 0L

val Long.plus3 by 3L

class C {
    inline operator fun Long.getValue(thisRef: IntArray, kProp: Any?) = thisRef[this.toInt()] * 10

    inline operator fun Long.setValue(thisRef: IntArray, kProp: Any?, newValue: Int) {
        thisRef[this.toInt()] = newValue * 10
    }

    inline operator fun Long.getValue(thisRef: Long, kProp: Any?) = this.toInt() + thisRef.toInt() * 10

    var IntArray.secondElementX10 by 1L
    
    val Long.appendDigit1 by 1L
    
    fun test(intArray: IntArray) {
        if (intArray.secondElementX10 != 220) throw AssertionError()
        intArray.secondElementX10 = 42
        if (intArray.secondElementX10 != 4200) throw AssertionError()

        if (42L.appendDigit1 != 421) throw AssertionError()
    }
}

fun box(): String {
    if (magic1 != 42) throw AssertionError()
    if (magic2 != 42) throw AssertionError()
    
    val intArray = IntArray(9) { (it + 1) * 10 + it + 1 } // [ 11, 22, 33, ..., 99 ]
    
    if (10L.plus3 != 13) throw AssertionError()
    
    if (intArray.firstElement != 11) throw AssertionError()
    intArray.firstElement = 42
    if (intArray.firstElement != 42) throw AssertionError()
    
    val x = C()
    x.test(intArray)
    
    return "OK"
}
