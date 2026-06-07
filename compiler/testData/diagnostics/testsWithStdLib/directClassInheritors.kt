// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
// WITH_COROUTINES
// LANGUAGE: +DirectClassInheritors, +MultiPlatformProjects

// MODULE: lib
// FILE: Lib.kt
package lib

abstract class Base

interface I<T>
// T -> {Int: [A], String: [B], I<T>: [C<T>]} | the concrete types of I<T> (for I<T>: [C<T>]) depend on the concrete types C<T> takes on for T
// T -> {Int: [A], String: [B], I<D<Int>>: [C<D<Int>>], I<D<F>>: [C<D<F>>]}

// MODULE: main(lib)
// FILE: Main.kt
package main

import lib.*

class Derived : Base(), I<Int>

class A : I<Int>
// <empty>

class B : I<String>
// <empty>

open class C<T> : I<I<T>>
// T -> {D<T>: [D<T>]} | the concrete types of D<T> (key) (for D<T>: [D<T>]) depend on the concrete types D<T> (value) takes on for T
// T -> {D<Int>: [D<Int>], D<F>: [D<F>]}

open class D<T> : C<D<T>>()
// T -> {Int: [E], F: [F, G]}

class E : D<Int>()
// <empty>

class F : D<F>()
// <empty>

class G : D<F>()
// <empty>

// FILE: Test.kt
import kotlin.coroutines.*

interface CoroutineTracerShim {
  companion object {
    var coroutineTracer: CoroutineTracerShim = object : CoroutineTracerShim {
      override fun rootTrace() = EmptyCoroutineContext
    }

    fun foo() {
        class Local : CoroutineTracerShim {
            override fun rootTrace() = EmptyCoroutineContext
        }
    }
  }

  fun rootTrace(): CoroutineContext
}

// MODULE: common
// FILE: Common.kt
interface MyDataItem {
    val id: String
    val text1: String
    val text2: String
}

// MODULE: platform(common)
// FILE: Platform.kt
fun createItemVM() = object {
    inner class MyItemVM(data: MyDataItem) : MyDataItem by data {
        val isSelected = false
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, assignment, checkNotNullCall, classDeclaration, companionObject,
functionDeclaration, functionalType, inheritanceDelegation, inner, interfaceDeclaration, lambdaLiteral, localClass,
localProperty, nullableType, objectDeclaration, override, primaryConstructor, propertyDeclaration, safeCall, suspend,
thisExpression, typeParameter */
