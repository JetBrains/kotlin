// IGNORE_BACKEND: JVM_IR
package test

import kotlin.reflect.KFunction
import kotlin.collections.*

@CompileTimeCalculation
fun withParameters(a: Int, b: Double) = 0
@CompileTimeCalculation
fun String.withExtension(a: Int) = 0

@CompileTimeCalculation
class A {
    fun String.get(a: Int) = this
}

const val parameters1 = (::withParameters as KFunction<*>).parameters.<!EVALUATED: `parameter #0 a of fun withParameters(kotlin.Int, kotlin.Double): kotlin.Int, parameter #1 b of fun withParameters(kotlin.Int, kotlin.Double): kotlin.Int`!>joinToString()<!>
const val parameters2 = (String::withExtension as KFunction<*>).parameters.<!EVALUATED: `extension receiver parameter of fun kotlin.String.withExtension(kotlin.Int): kotlin.Int, parameter #1 a of fun kotlin.String.withExtension(kotlin.Int): kotlin.Int`!>joinToString()<!>
//const val parameters3 = A::class.members.toList()[0].parameters.joinToString() TODO -> `instance parameter of fun test.A.(kotlin.String.)get(kotlin.Int): kotlin.String, extension receiver parameter of fun test.A.(kotlin.String.)get(kotlin.Int): kotlin.String, parameter #2 a of fun test.A.(kotlin.String.)get(kotlin.Int): kotlin.String`

// properties
@CompileTimeCalculation
class B(val b: Int) {
    val String.size: Int
        get() = this.length
}

const val property0Parameters = B(1)::b.parameters.<!EVALUATED: `[]`!>toString()<!>
const val property1Parameters = B::b.parameters.<!EVALUATED: `[instance parameter of val test.B.b: kotlin.Int]`!>toString()<!>
//const val property2Parameters = B::class.members.toList()[1].parameters.toString() TODO -> `[instance parameter of val test.B.(kotlin.String.)size: kotlin.Int, extension receiver parameter of val test.B.(kotlin.String.)size: kotlin.Int]`
