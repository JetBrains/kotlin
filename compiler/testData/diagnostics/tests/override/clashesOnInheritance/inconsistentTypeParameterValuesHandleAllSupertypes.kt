// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// ISSUE: KT-74147

// FILE: original.kt

package original

interface I0
interface I1<T0>: I0
interface I2: I1<I0>
class A1: <!INCONSISTENT_TYPE_PARAMETER_VALUES!>I2, I0, I1<A1><!>

// FILE: simple.kt

package simple

interface I0
interface I1<T> : I0
interface I2 : I1<Int>
interface A2 : <!INCONSISTENT_TYPE_PARAMETER_VALUES!>I1<Any>, I0, I2<!>

// FILE: clash.kt

package crash

interface I0
interface I1<T0>: I0 {
    fun consume(t: T0) {}
}
interface I2: I1<I0>
class A3: <!INCONSISTENT_TYPE_PARAMETER_VALUES!>I2, I0, I1<A3><!> // If it's allowed, it causes a platform declaration clash
