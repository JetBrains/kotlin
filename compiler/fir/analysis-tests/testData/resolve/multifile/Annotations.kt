// FILE: annotations.kt

package annotations

@Target(AnnotationTarget.FILE, AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.PROPERTY_GETTER)
annotation class Simple

annotation class WithInt(val value: Int)

annotation class WithString(val s: String)

annotation class Complex(val wi: WithInt, val ws: WithString)

annotation class VeryComplex(val f: Float, val d: Double, val b: Boolean, val l: Long, val n: Int?)

// FILE: main.kt

@file:Simple
package test

import annotations.*

@WithInt(42)
abstract class First {
    @Simple
    abstract fun foo(@WithString("abc") arg: @Simple Double)

    @Complex(WithInt(7), WithString(""))
    abstract val v: String
}

@WithString("xyz")
class Second(val y: Char) : @WithInt(0) First() {
    override fun foo(arg: Double) {
    }

    override val v: String
        @Simple get() = ""

    @WithString("constructor")
    constructor(): this('\n')
}

@WithInt(24)
@VeryComplex(3.14f, 6.67e-11, false, 123456789012345L, null)
typealias Third = @Simple Second