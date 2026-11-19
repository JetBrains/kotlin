// LANGUAGE: +MultiPlatformProjects
// DONT_TARGET_EXACT_BACKEND: JVM
// JVM is tested in boxModernJdk/testsWithJava11/volatilePropertyAtomics (VarHandle, JVM 9+) and box/volatilePropertyAtomicsJvm8
// (field updaters, JVM 1.8, which do not support static and Boolean properties).
// `@Volatile` is an optional expectation without actuals on JS and Wasm, so it can only be used in common code there.
// WITH_STDLIB
// OPT_IN: kotlin.concurrent.atomics.ExperimentalAtomicApi

// MODULE: common
// FILE: common.kt

import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.*

object InitOrder {
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

class Clash {
    @Volatile var x: Int = 1
    companion object {
        @Volatile var x: Int = 100
        fun inc() = this::x.incrementAndFetch()
    }
    fun incInstance() = this::x.incrementAndFetch()
}

fun commonBox(): String {
    if (InitOrder.y != 42) return "init order: ${InitOrder.y}"
    val b = Bounded<String>("a")
    if (b.swap("b") != "a" || !b.cas("b", "c") || b.v != "c") return "bounded"
    val o = Outer()
    if (o.Inner().inc() != 1) return "inner"
    if (o.viaLambda() != 7) return "lambda"
    if (Clash.inc() != 101) return "clash companion: ${Clash.x}"
    if (Clash().incInstance() != 2) return "clash instance"
    return "OK"
}

// MODULE: platform()()(common)
// FILE: platform.kt

fun box(): String = commonBox()
