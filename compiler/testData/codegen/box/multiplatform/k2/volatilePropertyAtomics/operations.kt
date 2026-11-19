// LANGUAGE: +MultiPlatformProjects
// DONT_TARGET_EXACT_BACKEND: JVM
// JVM is tested in boxModernJdk/testsWithJava11/volatilePropertyAtomics (VarHandle, JVM 9+) and box/volatilePropertyAtomicsJvm8
// (field updaters, JVM 1.8, which do not support Boolean properties).
// `@Volatile` is an optional expectation without actuals on JS and Wasm, so it can only be used in common code there.
// WITH_STDLIB
// OPT_IN: kotlin.concurrent.atomics.ExperimentalAtomicApi

// MODULE: common
// FILE: common.kt

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

class BooleanHolder {
    @Volatile var x: Boolean = false

    fun test(): String {
        if (x) return "Boolean: initial"
        if (!this::x.compareAndSet(false, true) || !x) return "Boolean: compareAndSet 1"
        if (this::x.compareAndSet(false, true)) return "Boolean: compareAndSet 2"
        if (!this::x.exchange(false) || x) return "Boolean: exchange"
        if (this::x.compareAndExchange(false, true) || !x) return "Boolean: compareAndExchange"
        x = false
        if (x) return "Boolean: store"
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

fun commonBox(): String {
    IntHolder().test().let { if (it != "OK") return it }
    LongHolder().test().let { if (it != "OK") return it }
    BooleanHolder().test().let { if (it != "OK") return it }
    StringHolder().test().let { if (it != "OK") return it }
    DataHolder().test().let { if (it != "OK") return it }
    testGeneric().let { if (it != "OK") return it }
    EvaluationOrder().test().let { if (it != "OK") return it }
    NonLocalReturn().test().let { if (it != "OK") return it }
    return "OK"
}

// MODULE: platform()()(common)
// FILE: platform.kt

fun box(): String = commonBox()
