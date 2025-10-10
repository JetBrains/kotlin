// LANGUAGE: +IntrinsicConstEvaluation
// IGNORE_BACKEND_K1: JVM_IR, JS_IR, JS_IR_ES6, NATIVE, WASM
// WITH_STDLIB
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or
import kotlin.experimental.xor

fun <T> T.id() = this

const val minusOneVal = (-1).<!EVALUATED("-1")!>toByte()<!>
const val zeroVal = 0.<!EVALUATED("0")!>toByte()<!>
const val oneVal = 1.<!EVALUATED("1")!>toByte()<!>
const val twoVal = 2.<!EVALUATED("2")!>toByte()<!>
const val threeVal = 3.<!EVALUATED("3")!>toByte()<!>
const val fourVal = 4.<!EVALUATED("4")!>toByte()<!>

const val and1 = oneVal.<!EVALUATED("0")!>and(twoVal)<!>
const val and2 = twoVal.<!EVALUATED("2")!>and(twoVal)<!>
const val and3 = threeVal.<!EVALUATED("2")!>and(twoVal)<!>
const val and4 = 12.toByte().<!EVALUATED("8")!>and(10.toByte())<!>

const val or1 = oneVal.<!EVALUATED("3")!>or(twoVal)<!>
const val or2 = twoVal.<!EVALUATED("2")!>or(twoVal)<!>
const val or3 = threeVal.<!EVALUATED("3")!>or(twoVal)<!>
const val or4 = 12.toByte().<!EVALUATED("14")!>or(10.toByte())<!>

const val xor1 = oneVal.<!EVALUATED("3")!>xor(twoVal)<!>
const val xor2 = twoVal.<!EVALUATED("0")!>xor(twoVal)<!>
const val xor3 = threeVal.<!EVALUATED("1")!>xor(twoVal)<!>
const val xor4 = 12.toByte().<!EVALUATED("6")!>xor(10.toByte())<!>

const val inv1 = zeroVal.<!EVALUATED("-1")!>inv()<!>
const val inv2 = oneVal.<!EVALUATED("-2")!>inv()<!>

// STOP_EVALUATION_CHECKS
fun box(): String {
    if (and1.id() !=  0.toByte())    return "and1"
    if (and2.id() !=  2.toByte())    return "and2"
    if (and3.id() !=  2.toByte())    return "and3"
    if (and4.id() !=  8.toByte())    return "and4"

    if (or1.id() !=  3.toByte())     return "or1"
    if (or2.id() !=  2.toByte())     return "or2"
    if (or3.id() !=  3.toByte())     return "or3"
    if (or4.id() !=  14.toByte())    return "or4"

    if (xor1.id() !=  3.toByte())    return "xor1"
    if (xor2.id() !=  0.toByte())    return "xor2"
    if (xor3.id() !=  1.toByte())    return "xor3"
    if (xor4.id() !=  6.toByte())    return "xor4"

    if (inv1.id() !=  (-1).toByte())    return "inv1"
    if (inv2.id() !=  (-2).toByte())    return "inv2"

    return "OK"
}
