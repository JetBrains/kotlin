/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

// All classes and methods should be used in tests
@file:Suppress("UNUSED")

package conversions

import kotlin.native.concurrent.isFrozen
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

// Constants
const val dbl: Double = 3.14
const val flt: Float = 2.73F
const val integer: Int = 42
const val longInt: Long = 1984

// Vars
var intVar: Int = 451
var str = "Kotlin String"
var strAsAny: Any = "Kotlin String as Any"

// MIN/MAX values as Numbers
var minDoubleVal: kotlin.Number = Double.MIN_VALUE
var maxDoubleVal: kotlin.Number = Double.MAX_VALUE

// Infinities and NaN
val nanDoubleVal: Double = Double.NaN
val nanFloatVal: Float = Float.NaN
val infDoubleVal: Double = Double.POSITIVE_INFINITY
val infFloatVal: Float = Float.NEGATIVE_INFINITY

private fun <T> T.toNullable(): T? = this

fun box(booleanValue: Boolean) = booleanValue.toNullable()
fun box(byteValue: Byte) = byteValue.toNullable()
fun box(shortValue: Short) = shortValue.toNullable()
fun box(intValue: Int) = intValue.toNullable()
fun box(longValue: Long) = longValue.toNullable()
fun box(uByteValue: UByte) = uByteValue.toNullable()
fun box(uShortValue: UShort) = uShortValue.toNullable()
fun box(uIntValue: UInt) = uIntValue.toNullable()
fun box(uLongValue: ULong) = uLongValue.toNullable()
fun box(floatValue: Float) = floatValue.toNullable()
fun box(doubleValue: Double) = doubleValue.toNullable()

private inline fun <reified T> ensureEquals(actual: T?, expected: T) {
    if (actual !is T) error(T::class)
    if (actual != expected) error(T::class)
}

fun ensureEqualBooleans(actual: Boolean?, expected: Boolean) = ensureEquals(actual, expected)
fun ensureEqualBytes(actual: Byte?, expected: Byte) = ensureEquals(actual, expected)
fun ensureEqualShorts(actual: Short?, expected: Short) = ensureEquals(actual, expected)
fun ensureEqualInts(actual: Int?, expected: Int) = ensureEquals(actual, expected)
fun ensureEqualLongs(actual: Long?, expected: Long) = ensureEquals(actual, expected)
fun ensureEqualUBytes(actual: UByte?, expected: UByte) = ensureEquals(actual, expected)
fun ensureEqualUShorts(actual: UShort?, expected: UShort) = ensureEquals(actual, expected)
fun ensureEqualUInts(actual: UInt?, expected: UInt) = ensureEquals(actual, expected)
fun ensureEqualULongs(actual: ULong?, expected: ULong) = ensureEquals(actual, expected)
fun ensureEqualFloats(actual: Float?, expected: Float) = ensureEquals(actual, expected)
fun ensureEqualDoubles(actual: Double?, expected: Double) = ensureEquals(actual, expected)

// Boolean
val boolVal: Boolean = true
val boolAnyVal: Any = false

// Lists
val numbersList: List<Number> = listOf(1.toByte(), 2.toShort(), 13)
val anyList: List<Any> = listOf("Str", 42, 3.14, true)

// lateinit
lateinit var lateinitIntVar: Any

// lazy
val lazyVal: String by lazy {
    println("Lazy value initialization")
    "Lazily initialized string"
}

// Delegation
var delegatedGlobalArray: Array<String> by DelegateClass()

class DelegateClass: ReadWriteProperty<Nothing?, Array<String>> {

    private var holder: Array<String> = arrayOf("property")

    override fun getValue(thisRef: Nothing?, property: KProperty<*>): Array<String> {
        return arrayOf("Delegated", "global", "array") + holder
    }

    override fun setValue(thisRef: Nothing?, property: KProperty<*>, value: Array<String>) {
        holder = value
    }
}

// Getter with delegation
val delegatedList: List<String>
    get() = delegatedGlobalArray.toList()

// Null
val nullVal: Any? = null
var nullVar: String? = ""

// Any
var anyValue: Any = "Str"

// Functions
fun emptyFun() { }

fun strFun(): String = "fooStr"

fun argsFun(i: Int, l: Long, d: Double, s: String): Any = s + i + l + d

fun funArgument(foo: () -> String): String = foo()

// Generic functions
fun <T, R> genericFoo(t: T, foo: (T) -> R): R = foo(t)

fun <T : Number, R : T> fooGenericNumber(r: R, foo: (T) -> Number): Number = foo(r)

fun <T> varargToList(vararg args: T): List<T> = args.toList()

// Extensions
fun String.subExt(i: Int): String {
    return if (i < this.length) this[i].toString() else "nothing"
}

fun Any?.toString(): String = this?.toString() ?: "null"

fun Any?.print() = println(this.toString())

fun Char.boxChar(): Char? = this
fun Char?.isA(): Boolean = (this == 'A')

// Lambdas
val sumLambda = { x: Int, y: Int -> x + y }


// Inheritance
interface I {
    fun iFun(): String = "I::iFun"
}

private interface PI {
    fun piFun(): Any
    fun iFun(): String = "PI::iFun"
}

class DefaultInterfaceExt : I

open class OpenClassI : I {
    override fun iFun(): String = "OpenClassI::iFun"
}

class FinalClassExtOpen : OpenClassI() {
    override fun iFun(): String = "FinalClassExtOpen::iFun"
}

open class MultiExtClass : OpenClassI(), PI {
    override fun piFun(): Any {
        return 42
    }

    override fun iFun(): String = super<PI>.iFun()
}

open class ConstrClass(open val i: Int, val s: String, val a: Any = "AnyS") : OpenClassI()

class ExtConstrClass(override val i: Int) : ConstrClass(i, "String") {
    override fun iFun(): String  = "ExtConstrClass::iFun::$i-$s-$a"
}

// Enum
enum class Enumeration(val enumValue: Int) {
    ANSWER(42), YEAR(1984), TEMPERATURE(451)
}

fun passEnum(): Enumeration {
    return Enumeration.ANSWER
}

fun receiveEnum(e: Int) {
    println("ENUM got: ${get(e).enumValue}")
}

fun get(value: Int): Enumeration {
    return Enumeration.values()[value]
}

// Data class values and generated properties: component# and toString()
data class TripleVals<T>(val first: T, val second: T, val third: T)

data class TripleVars<T>(var first: T, var second: T, var third: T) {
    override fun toString(): String {
        return "[$first, $second, $third]"
    }
}

open class WithCompanionAndObject {
    companion object {
        val str = "String"
        var named: I? = Named
    }

    object Named : OpenClassI() {
        override fun iFun(): String = "WithCompanionAndObject.Named::iFun"
    }
}

fun getCompanionObject() = WithCompanionAndObject.Companion
fun getNamedObject() = WithCompanionAndObject.Named
fun getNamedObjectInterface(): OpenClassI = WithCompanionAndObject.Named

typealias EE = Enumeration
fun EE.getAnswer() : EE  = Enumeration.ANSWER

inline class IC1(val value: Int)
inline class IC2(val value: String)
inline class IC3(val value: TripleVals<Any?>?)

fun box(ic1: IC1): Any = ic1
fun box(ic2: IC2): Any = ic2
fun box(ic3: IC3): Any = ic3

fun concatenateInlineClassValues(ic1: IC1, ic1N: IC1?, ic2: IC2, ic2N: IC2?, ic3: IC3, ic3N: IC3?): String =
        "${ic1.value} ${ic1N?.value} ${ic2.value} ${ic2N?.value} ${ic3.value} ${ic3N?.value}"

fun IC1.getValue1() = this.value
fun IC1?.getValueOrNull1() = this?.value

fun IC2.getValue2() = value
fun IC2?.getValueOrNull2() = this?.value

fun IC3.getValue3() = value
fun IC3?.getValueOrNull3() = this?.value

fun isFrozen(obj: Any): Boolean = obj.isFrozen
fun kotlinLambda(block: (Any) -> Any): Any = block

fun multiply(int: Int, long: Long) = int * long

class MyException : Exception()

open class BridgeBase {
    @Throws
    open fun foo1(): Any = Any()
    @Throws
    open fun foo2(): Int = 42
    @Throws
    open fun foo3(): Unit = Unit
    @Throws
    open fun foo4(): Nothing? = throw IllegalStateException()
}

class Bridge : BridgeBase() {
    override fun foo1() = throw MyException()
    override fun foo2() = throw MyException()
    override fun foo3() = throw MyException()
    override fun foo4() = throw MyException()
}