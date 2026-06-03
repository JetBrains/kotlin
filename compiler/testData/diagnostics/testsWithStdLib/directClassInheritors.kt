// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
// WITH_COROUTINES
// LANGUAGE: +DirectClassInheritors, +MultiPlatformProjects

// MODULE: lib
// FILE: Lib.kt
package lib

abstract class A
interface I<T>

// MODULE: main(lib)
// FILE: Main.kt
package main

import lib.*
class B : A(), I<Int>

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
