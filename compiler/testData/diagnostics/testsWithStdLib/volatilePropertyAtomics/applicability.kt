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

object Obj {
    @Volatile var x: Long = 0L
    fun ok() = this::x.incrementAndFetch()
}

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
        ::topLevel.incrementAndFetch()
        this::i.compareAndSet(0, 1)
        this::l.fetchAndAdd(1L)
        this::b.exchange(true)
        this::s.compareAndExchange("", "a")
        this::i += 1
        this::l -= 1L
        this::i.update { it + 1 }
        val r: String = this::s.updateAndFetch { it + "!" }
        Foo()::i.load()
        (this::i).store(42)
        this::withPrivateSetter.store(1)

        object {
            fun local() {
                class Local {
                    val xx = this@Foo::i.load()
                }
            }
        }
    }

    fun onNonLiteral() {
        val p = this::i
        p.<!VOLATILE_INTRINSIC_CALL_ON_NON_LITERAL!>load<!>()
    }

    fun onNonVolatile() {
        this::nonVolatile.<!VOLATILE_INTRINSIC_CALL_ON_NON_VOLATILE!>load<!>()
        ::nonVolatileTopLevel.<!VOLATILE_INTRINSIC_CALL_ON_NON_VOLATILE!>incrementAndFetch<!>()
        this::delegated.<!VOLATILE_INTRINSIC_CALL_ON_NON_VOLATILE!>load<!>()
    }

    fun onCustomAccessors() {
        this::withCustomGetter.<!VOLATILE_INTRINSIC_CALL_ON_PROPERTY_WITH_CUSTOM_ACCESSORS!>load<!>()
    }

    fun onUnsupportedType() {
        this::short.<!VOLATILE_INTRINSIC_CALL_ON_UNSUPPORTED_TYPE!>compareAndSet<!>(0, 1)
        this::nullableInt.<!VOLATILE_INTRINSIC_CALL_ON_UNSUPPORTED_TYPE!>compareAndSet<!>(null, 1)
        this::uint.<!VOLATILE_INTRINSIC_CALL_ON_UNSUPPORTED_TYPE!>load<!>()
        this::i.<!VOLATILE_INTRINSIC_CALL_ON_UNSUPPORTED_TYPE!>compareAndSet<!><Int>(0, 1)
    }

    inline fun inlineFun() {
        this::i.<!VOLATILE_INTRINSIC_CALL_IN_INLINE_FUNCTION!>load<!>()
        run { this::i.<!VOLATILE_INTRINSIC_CALL_IN_INLINE_FUNCTION!>load<!>() }
    }

    inner class InnerSubclass : Foo() {
        fun innerOk() {
            this@Foo::i.load()
            // Access to `InnerSubclass.i`, which is inherited from `Foo.i` declared lexically above.
            this::i.load()
        }
    }
}

class Subclass : Foo() {
    fun notAccessible() {
        this::i.<!VOLATILE_INTRINSIC_CALL_ON_NON_ACCESSIBLE_PROPERTY!>load<!>()
    }
}

class Other {
    fun notAccessible(foo: Foo) {
        foo::i.<!VOLATILE_INTRINSIC_CALL_ON_NON_ACCESSIBLE_PROPERTY!>load<!>()
        Obj::x.<!VOLATILE_INTRINSIC_CALL_ON_NON_ACCESSIBLE_PROPERTY!>load<!>()
    }
}

class Delegate {
    operator fun getValue(thisRef: Any?, property: Any?): Int = 0
    operator fun setValue(thisRef: Any?, property: Any?, value: Int) {}
}
