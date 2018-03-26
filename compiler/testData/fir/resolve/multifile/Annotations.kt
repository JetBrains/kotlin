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
typealias Third = @Simple Second