/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// All classes and methods should be used in tests
@file:Suppress("UNUSED")

package conversions

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
