// LANGUAGE: +IntrinsicConstEvaluation
// IGNORE_BACKEND_K1: JVM_IR, JS_IR, JS_IR_ES6, NATIVE, WASM

fun <T> T.id() = this

const val longVal1 = 2L.<!EVALUATED("3")!>inc()<!>
const val longVal2 = 2L.<!EVALUATED("1")!>dec()<!>
const val longVal3 = Long.MAX_VALUE.<!EVALUATED("-9223372036854775808")!>inc()<!>
const val longVal4 = Long.MIN_VALUE.<!EVALUATED("9223372036854775807")!>dec()<!>

const val intVal1 = 2.<!EVALUATED("3")!>inc()<!>
const val intVal2 = 2.<!EVALUATED("1")!>dec()<!>
const val intVal3 = Int.MAX_VALUE.<!EVALUATED("-2147483648")!>inc()<!>
const val intVal4 = Int.MIN_VALUE.<!EVALUATED("2147483647")!>dec()<!>

const val shortVal1 = 2.toShort().<!EVALUATED("3")!>inc()<!>
const val shortVal2 = 2.toShort().<!EVALUATED("1")!>dec()<!>
const val shortVal3 = Short.MAX_VALUE.<!EVALUATED("-32768")!>inc()<!>
const val shortVal4 = Short.MIN_VALUE.<!EVALUATED("32767")!>dec()<!>

const val byteVal1 = 2.toByte().<!EVALUATED("3")!>inc()<!>
const val byteVal2 = 2.toByte().<!EVALUATED("1")!>dec()<!>
const val byteVal3 = Byte.MAX_VALUE.<!EVALUATED("-128")!>inc()<!>
const val byteVal4 = Byte.MIN_VALUE.<!EVALUATED("127")!>dec()<!>

// STOP_EVALUATION_CHECKS
fun box(): String {
    if (longVal1.id() != 3L)                    return "Fail longVal1"
    if (longVal2.id()!= 1L)                     return "Fail longVal2"
    if (longVal3.id() != Long.MIN_VALUE)        return "Fail longVal3"
    if (longVal4.id() != Long.MAX_VALUE)        return "Fail longVal4"

    if (intVal1.id() != 3)                      return "Fail intVal1"
    if (intVal2.id() != 1)                      return "Fail intVal2"
    if (intVal3.id() != Int.MIN_VALUE)          return "Fail intVal3"
    if (intVal4.id() != Int.MAX_VALUE)          return "Fail intVal4"

    if (shortVal1.id() != 3.toShort())          return "Fail shortVal1"
    if (shortVal2.id() != 1.toShort())          return "Fail shortVal2"
    if (shortVal3.id() != Short.MIN_VALUE)      return "Fail shortVal3"
    if (shortVal4.id() != Short.MAX_VALUE)      return "Fail shortVal4"

    if (byteVal1.id() != 3.toByte())            return "Fail byteVal1"
    if (byteVal2.id() != 1.toByte())            return "Fail byteVal2"
    if (byteVal3.id() != Byte.MIN_VALUE)        return "Fail byteVal3"
    if (byteVal4.id() != Byte.MAX_VALUE)        return "Fail byteVal4"

    return "OK"
}
