// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K2: JVM_IR, JS_IR, JS_IR_ES6
// FIR status: KT-46419, ILT conversions to Byte and Short are not supported by design
// SKIP_JDK6
// FULL_JDK

import java.util.function.Supplier

class A {
    val xLong: Supplier<Long>
        get() = Supplier { 30 * 30 }

    val xShort: Supplier<Short>
        get() = Supplier { 30 * 30 }

    val xInt: Supplier<Int>
        get() = Supplier { 30 * 30 }

    val xByte: Supplier<Byte>
        get() = Supplier { 3 * 3 }
}

fun box(): String {
    val a = A()
    if (a.xLong.get() != 900L) return "FAIL1"
    var expectedShort: Short = 900
    if (a.xShort.get() != expectedShort) return "FAIL2"
    if (a.xInt.get() != 900) return "FAIL3"
    val expectedByte: Byte = 9
    if (a.xByte.get() != expectedByte) return "FAIL4"
    return "OK"
}
