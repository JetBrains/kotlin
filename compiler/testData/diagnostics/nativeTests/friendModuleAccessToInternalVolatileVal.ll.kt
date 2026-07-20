// LL_FIR_DIVERGENCE
// We report `LEAKED_VOLATILE_FIELD` after fir2ir stage before serialization in Klib
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: BACKEND
// DISABLE_IR_VISIBILITY_CHECKS: ANY
// DIAGNOSTICS: -ERROR_SUPPRESSION -NOTHING_TO_INLINE

// MODULE: lib
// FILE: lib.kt
@file:OptIn(kotlin.ExperimentalStdlibApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

import kotlin.native.concurrent.*
import kotlin.concurrent.*

@Volatile
internal var v: Int = 0

internal fun set(newValue: Int) {
    ::v.atomicSetField(newValue)
}

inline internal fun compareAndSet(expected: Int, newValue: Int): Boolean =
    ::v.compareAndSetField(expected, newValue)

class C {
    @Volatile
    internal var v: Int = 0

    internal fun getAndSet(newValue: Int): Int =
        this::v.getAndSetField(newValue)

    inline internal fun getAndAdd(delta: Int): Int =
        this::v.getAndAddField(delta)
}

// MODULE: test()(lib)
// FILE: test.kt
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

import kotlin.native.concurrent.*
import kotlin.concurrent.*

fun box(): String {
      v
      ::v.atomicGetField()
      set(1)
      compareAndSet(1, 2)

      val c = C()
      c.v
      c::v.compareAndExchangeField(0, 1)
      c.getAndSet(2)
      c.getAndAdd(3)

      return "OK"
}
