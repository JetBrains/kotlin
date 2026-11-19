// TARGET_BACKEND: JVM
// JVM_TARGET: 11
// WITH_STDLIB
// OPT_IN: kotlin.concurrent.atomics.ExperimentalAtomicApi

import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.*

// The VarHandle field is initialized in <clinit>, before any instance initializer can use it.
class InitOrder {
    @Volatile var x: Int = 0
    val y: Int
    init {
        this::x.compareAndSet(0, 41)
        y = this::x.incrementAndFetch()
    }
}

class Bounded<T : CharSequence>(@Volatile var v: T) {
    fun swap(new: T): T = this::v.exchange(new)
    fun cas(e: T, n: T): Boolean = this::v.compareAndSet(e, n)
}

class Outer {
    @Volatile var counter: Int = 0
    inner class Inner {
        fun inc() = this@Outer::counter.incrementAndFetch()
    }
    fun viaLambda(): Int = listOf(1, 2, 3).map { this::counter.addAndFetch(it) }.last()
}

// The instance field `x` clashes with the static field `x` moved from the companion, so one of them is renamed;
// the field name passed to `getDeclaredField` must follow.
class Clash {
    @Volatile var x: Int = 1
    companion object {
        @Volatile var x: Int = 100
    }
    fun incInstance() = this::x.incrementAndFetch()
}

fun box(): String {
    if (InitOrder().y != 42) return "init order"
    val b = Bounded<String>("a")
    if (b.swap("b") != "a" || !b.cas("b", "c") || b.v != "c") return "bounded"
    val o = Outer()
    if (o.Inner().inc() != 1) return "inner"
    if (o.viaLambda() != 7) return "lambda"
    if (Clash().incInstance() != 2 || Clash.x != 100) return "clash instance"
    return "OK"
}
