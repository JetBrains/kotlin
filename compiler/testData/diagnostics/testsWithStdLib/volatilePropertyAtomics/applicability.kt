// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.concurrent.atomics.ExperimentalAtomicApi
// DIAGNOSTICS: -UNUSED_VARIABLE -NOTHING_TO_INLINE
// FILE: test.kt

import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.*

@Volatile var topLevel: Int = 0
var nonVolatileTopLevel: Int = 0

class Box<T>(@Volatile var value: T) {
    fun ok(new: T) = this::value.exchange(new)
}

// Properties stored in static fields are prohibited: they are allocated once, so `Atomic*` classes can be used instead.
object Obj {
    @Volatile var x: Long = 0L
    fun onStatic() = this::x.<!VOLATILE_INTRINSIC_CALL_ON_STATIC_PROPERTY!>incrementAndFetch<!>()
}

class WithCompanion {
    companion object {
        @Volatile var c: Int = 0
        fun onStatic() {
            this::c.<!VOLATILE_INTRINSIC_CALL_ON_STATIC_PROPERTY!>exchange<!>(0)
            WithCompanion::c.<!VOLATILE_INTRINSIC_CALL_ON_STATIC_PROPERTY!>compareAndSet<!>(0, 1)
        }
    }
}

fun onStaticTopLevel() {
    ::topLevel.<!VOLATILE_INTRINSIC_CALL_ON_STATIC_PROPERTY!>exchange<!>(1)
    ::topLevel.<!VOLATILE_INTRINSIC_CALL_ON_STATIC_PROPERTY!>incrementAndFetch<!>()
}

// Properties of anonymous objects are stored in instance fields.
fun anonymousObjectOk() = object {
    @Volatile var x: Int = 0
    fun inc() = this::x.incrementAndFetch()
}.inc()

open class Foo {
    @Volatile var i: Int = 0
    @Volatile var l: Long = 0L
    @Volatile var b: Boolean = false
    @Volatile var s: String = ""
    @Volatile var nullableInt: Int? = null
    @Volatile var short: Short = 0
    @Volatile var uint: UInt = 0u
    var nonVolatile: Int = 0
    var delegated: Int by Delegate()
    @Volatile var withCustomGetter: Int = 0
        get() = field + 1
    @Volatile var withPrivateSetter: Int = 0
        private set

    fun ok() {
        this::i.compareAndSet(0, 1)
        this::l.fetchAndAdd(1L)
        this::b.exchange(true)
        this::s.compareAndExchange("", "a")
        this::i += 1
        this::l -= 1L
        this::i.update { it + 1 }
        val r: String = this::s.updateAndFetch { it + "!" }
        Foo()::i.exchange(0)
        (this::i).exchange(42)
        this::withPrivateSetter.exchange(1)

        object {
            fun local() {
                class Local {
                    val xx = this@Foo::i.exchange(0)
                }
            }
        }
    }

    fun onNonLiteral() {
        val p = this::i
        p.<!VOLATILE_INTRINSIC_CALL_ON_NON_LITERAL!>exchange<!>(0)
    }

    fun onNonVolatile() {
        this::nonVolatile.<!VOLATILE_INTRINSIC_CALL_ON_NON_VOLATILE!>exchange<!>(0)
        ::nonVolatileTopLevel.<!VOLATILE_INTRINSIC_CALL_ON_NON_VOLATILE!>incrementAndFetch<!>()
        this::delegated.<!VOLATILE_INTRINSIC_CALL_ON_NON_VOLATILE!>exchange<!>(0)
    }

    fun onCustomAccessors() {
        this::withCustomGetter.<!VOLATILE_INTRINSIC_CALL_ON_PROPERTY_WITH_CUSTOM_ACCESSORS!>exchange<!>(0)
    }

    fun onUnsupportedType() {
        this::short.<!VOLATILE_INTRINSIC_CALL_ON_UNSUPPORTED_TYPE!>compareAndSet<!>(0, 1)
        this::nullableInt.<!VOLATILE_INTRINSIC_CALL_ON_UNSUPPORTED_TYPE!>compareAndSet<!>(null, 1)
        this::uint.<!VOLATILE_INTRINSIC_CALL_ON_UNSUPPORTED_TYPE!>exchange<!>(0u)
        this::i.<!VOLATILE_INTRINSIC_CALL_ON_UNSUPPORTED_TYPE!>compareAndSet<!><Int>(0, 1)
    }

    inline fun inlineFun() {
        this::i.<!VOLATILE_INTRINSIC_CALL_IN_INLINE_FUNCTION!>exchange<!>(0)
        run { this::i.<!VOLATILE_INTRINSIC_CALL_IN_INLINE_FUNCTION!>exchange<!>(0) }
    }

    inner class InnerSubclass : Foo() {
        fun innerOk() {
            this@Foo::i.exchange(0)
            // Access to `InnerSubclass.i`, which is inherited from `Foo.i` declared lexically above.
            this::i.exchange(0)
        }
    }
}

class Subclass : Foo() {
    fun notAccessible() {
        this::i.<!VOLATILE_INTRINSIC_CALL_ON_NON_ACCESSIBLE_PROPERTY!>exchange<!>(0)
    }
}

class Other {
    fun notAccessible(foo: Foo) {
        foo::i.<!VOLATILE_INTRINSIC_CALL_ON_NON_ACCESSIBLE_PROPERTY!>exchange<!>(0)
    }
}

class Delegate {
    operator fun getValue(thisRef: Any?, property: Any?): Int = 0
    operator fun setValue(thisRef: Any?, property: Any?, value: Int) {}
}
