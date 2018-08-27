/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlinx.cinterop.*

import kotlin.native.CName

// Top level functions.
fun hello() {
    println("Hello, dynamic!")
}

fun getString() = "Kotlin/Native"

// Class with inheritance.
open class Base {
    open fun foo() = println("Base.foo")

    open fun fooParam(arg0: String, arg1: Int) = println("Base.fooParam: $arg0 $arg1")

    @CName(externName = "", shortName = "strangeName") fun странноеИмя() = 111

}

// Top level functions.
@CName(externName = "topLevelFunctionFromC", shortName = "topLevelFunctionFromCShort")
fun topLevelFunction(x1: Int, x2: Int) = x1 - x2

@CName("topLevelFunctionVoidFromC")
fun topLevelFunctionVoid(x1: Int, pointer: COpaquePointer?) {
    assert(x1 == 42)
    assert(pointer == null)
}

// Enum.
enum class Enum(val code: Int) {
    ONE(1),
    TWO(2),
    HUNDRED(100)
}

// Object.
interface Codeable {
    fun asCode(): Int
}

val an_object = object : Codeable {
    override fun asCode() = 42
}

object Singleton {
    override fun toString() = "I am single"
}

class Child : Base() {
    override fun fooParam(arg0: String, arg1: Int) = println("Child.fooParam: $arg0 $arg1")

    val roProperty: Int
        get() = 42

    var rwProperty: Int = 0
        get() = field
        set(value) { field = value + 1 }
}

// Interface.
interface I {
    fun foo(arg0: String, arg1: Int, arg2: I)
    fun fooImpl() = foo("Hi", 239, this)
}

open class Impl1: I {
    override fun foo(arg0: String, arg1: Int, arg2: I) {
        println("Impl1.I: $arg0 $arg1 ${arg2::class.qualifiedName}")
    }
}

class Impl2 : Impl1() {
    override fun foo(arg0: String, arg1: Int, arg2: I) {
        println("Impl2.I: $arg0 $arg1 ${arg2::class.qualifiedName}")
    }
}

inline class IC1(val value: Int)
inline class IC2(val value: String)
inline class IC3(val value: Base?)

fun useInlineClasses(ic1: IC1, ic2: IC2, ic3: IC3) {
    assert(ic1.value == 42)
    assert(ic2.value == "bar")
    assert(ic3.value is Base)
}
