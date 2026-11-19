// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// JVM 1.8 has no VarHandle: atomic operations are implemented with Atomic*FieldUpdater, see FieldUpdaterVolatilePropertyAtomicsLowering.
// Static and Boolean properties are not supported there, see diagnostics/tests/jvm/volatilePropertyAtomicsJvm8Limitations.kt.
// WITH_STDLIB
// OPT_IN: kotlin.concurrent.atomics.ExperimentalAtomicApi

import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.*

class IntHolder {
    @Volatile var x: Int = 0

    fun test(): String {
        if (x != 0) return "Int: initial"
        x = 5
        if (x != 5) return "Int: store"
        if (this::x.exchange(7) != 5 || x != 7) return "Int: exchange"
        if (!this::x.compareAndSet(7, 8) || x != 8) return "Int: compareAndSet 1"
        if (this::x.compareAndSet(7, 9) || x != 8) return "Int: compareAndSet 2"
        if (this::x.compareAndExchange(8, 10) != 8 || x != 10) return "Int: compareAndExchange 1"
        if (this::x.compareAndExchange(8, 11) != 10 || x != 10) return "Int: compareAndExchange 2"
        if (this::x.fetchAndAdd(5) != 10 || x != 15) return "Int: fetchAndAdd"
        if (this::x.addAndFetch(5) != 20 || x != 20) return "Int: addAndFetch"
        if (this::x.fetchAndIncrement() != 20 || x != 21) return "Int: fetchAndIncrement"
        if (this::x.incrementAndFetch() != 22 || x != 22) return "Int: incrementAndFetch"
        if (this::x.fetchAndDecrement() != 22 || x != 21) return "Int: fetchAndDecrement"
        if (this::x.decrementAndFetch() != 20 || x != 20) return "Int: decrementAndFetch"
        this::x += 10
        if (x != 30) return "Int: plusAssign"
        this::x -= 5
        if (x != 25) return "Int: minusAssign"
        this::x.update { it * 2 }
        if (x != 50) return "Int: update"
        if (this::x.fetchAndUpdate { it + 1 } != 50 || x != 51) return "Int: fetchAndUpdate"
        if (this::x.updateAndFetch { it + 1 } != 52 || x != 52) return "Int: updateAndFetch"
        return "OK"
    }
}

class LongHolder {
    @Volatile var x: Long = 0L

    fun test(): String {
        x = Long.MAX_VALUE - 1
        if (this::x.incrementAndFetch() != Long.MAX_VALUE) return "Long: incrementAndFetch"
        if (this::x.fetchAndAdd(-Long.MAX_VALUE) != Long.MAX_VALUE || x != 0L) return "Long: fetchAndAdd"
        if (!this::x.compareAndSet(0L, 1L shl 40)) return "Long: compareAndSet"
        if (this::x.compareAndExchange(1L shl 40, 3L) != 1L shl 40) return "Long: compareAndExchange"
        this::x += 2L
        this::x -= 1L
        if (x != 4L) return "Long: plusAssign/minusAssign"
        if (this::x.updateAndFetch { it * 1_000_000_000_000L } != 4_000_000_000_000L) return "Long: updateAndFetch"
        if (this::x.decrementAndFetch() != 3_999_999_999_999L) return "Long: decrementAndFetch"
        return "OK"
    }
}

class StringHolder {
    @Volatile var x: String = "a"

    fun test(): String {
        val a = x
        val b = a + "b"
        if (!this::x.compareAndSet(a, b) || x != "ab") return "String: compareAndSet 1"
        if (this::x.exchange("c") !== b) return "String: exchange"
        this::x.update { it + "d" }
        if (this::x.fetchAndUpdate { it + "e" } != "cd") return "String: fetchAndUpdate"
        if (this::x.updateAndFetch { it.uppercase() } != "CDE") return "String: updateAndFetch"
        return "OK"
    }
}

data class Data(val value: Int)

class DataHolder {
    @Volatile var x: Data = Data(1)

    fun test(): String {
        // Comparison is done by reference
        val equalButNotSame = Data(1)
        if (this::x.compareAndSet(equalButNotSame, Data(2))) return "Reference: compareAndSet by equality"
        if (this::x.compareAndExchange(equalButNotSame, Data(2)) != Data(1) || x != Data(1)) return "Reference: compareAndExchange"
        val same = x
        if (!this::x.compareAndSet(same, Data(3)) || x.value != 3) return "Reference: compareAndSet"
        return "OK"
    }
}

class GenericHolder<T>(@Volatile var x: T) {
    fun swap(new: T): T = this::x.exchange(new)
    fun cas(expected: T, new: T): Boolean = this::x.compareAndSet(expected, new)
    fun transform(f: (T) -> T): T = this::x.updateAndFetch(f)
}

fun testGeneric(): String {
    val h = GenericHolder<List<Int>?>(null)
    val list = listOf(1)
    if (h.swap(list) != null) return "Generic: exchange"
    if (!h.cas(list, listOf(2))) return "Generic: compareAndSet"
    if (h.transform { it.orEmpty() + 3 } != listOf(2, 3)) return "Generic: updateAndFetch"
    return "OK"
}

class EvaluationOrder {
    @Volatile var x: Int = 0
    val log = StringBuilder()

    fun self(): EvaluationOrder { log.append("r"); return this }
    fun arg(v: Int): Int { log.append(v); return v }

    fun test(): String {
        self()::x.compareAndSet(arg(0), arg(1))
        self()::x.updateAndFetch { log.append("t"); it + 1 }
        if (log.toString() != "r01rt") return "evaluation order: $log"
        if (x != 2) return "evaluation order: x = $x"
        return "OK"
    }
}

class NonLocalReturn {
    @Volatile var x: Int = 0

    fun test(): String {
        this::x.update {
            if (it == 0) return "OK"
            it
        }
        return "Fail: non-local return"
    }
}

class CompareAndExchangeEmulation {
    @Volatile var i: Int = Int.MIN_VALUE
    @Volatile var l: Long = Long.MIN_VALUE
    @Volatile var s: String? = null

    fun test(): String {
        // The witness is returned both on success and on failure.
        if (this::i.compareAndExchange(Int.MIN_VALUE, -1) != Int.MIN_VALUE || i != -1) return "cmpxchg Int success"
        if (this::i.compareAndExchange(Int.MIN_VALUE, 0) != -1 || i != -1) return "cmpxchg Int failure"
        if (this::l.compareAndExchange(Long.MIN_VALUE, -1L) != Long.MIN_VALUE || l != -1L) return "cmpxchg Long success"
        if (this::l.compareAndExchange(0L, 1L) != -1L || l != -1L) return "cmpxchg Long failure"
        // References are compared by identity, nulls are supported.
        val a = "a"
        if (this::s.compareAndExchange(null, a) != null || s !== a) return "cmpxchg null success"
        if (this::s.compareAndExchange(null, "b") !== a || s !== a) return "cmpxchg null failure"
        val notSameA = StringBuilder("a").toString()
        if (this::s.compareAndExchange(notSameA, "b") !== a || s !== a) return "cmpxchg identity"
        if (this::s.compareAndExchange(a, null) !== a || s != null) return "cmpxchg to null"
        return "OK"
    }
}

class Outer {
    @Volatile var counter: Int = 0
    inner class Inner {
        fun inc() = this@Outer::counter.incrementAndFetch()
    }
    fun viaLambda(): Int = listOf(1, 2, 3).map { this::counter.addAndFetch(it) }.last()
}

// The instance field `x` clashes with the static field `x` moved from the companion, so one of them is renamed;
// the field name passed to `newUpdater` / `getDeclaredField` must follow.
class Clash {
    @Volatile var x: Int = 1
    @Volatile var s: String = "a"
    companion object {
        @Volatile var x: Int = 100
        @Volatile var s: String = "static"
    }
    fun incInstance() = this::x.incrementAndFetch()
    fun casInstance() = this::s.compareAndSet("a", "b")
}

fun testClash(): String {
    val c = Clash()
    if (c.incInstance() != 2 || c.x != 2 || Clash.x != 100) return "clash Int"
    if (!c.casInstance() || c.s != "b" || Clash.s != "static") return "clash String"
    return "OK"
}

fun testOuter(): String {
    val o = Outer()
    if (o.Inner().inc() != 1) return "inner"
    if (o.viaLambda() != 7) return "lambda"
    return "OK"
}

fun box(): String {
    IntHolder().test().let { if (it != "OK") return it }
    LongHolder().test().let { if (it != "OK") return it }
    StringHolder().test().let { if (it != "OK") return it }
    DataHolder().test().let { if (it != "OK") return it }
    testGeneric().let { if (it != "OK") return it }
    EvaluationOrder().test().let { if (it != "OK") return it }
    NonLocalReturn().test().let { if (it != "OK") return it }
    CompareAndExchangeEmulation().test().let { if (it != "OK") return it }
    testOuter().let { if (it != "OK") return it }
    testClash().let { if (it != "OK") return it }
    return "OK"
}
