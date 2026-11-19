// JVM_TARGET: 11
// WITH_STDLIB
// OPT_IN: kotlin.concurrent.atomics.ExperimentalAtomicApi

import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.*

class A {
    @Volatile var i: Int = 0
    @Volatile var l: Long = 0L
    @Volatile var s: String = ""

    fun cas(): Boolean = this::i.compareAndSet(0, 1)
    fun increment(): Long = this::l.incrementAndFetch()
    fun exchange(): String = this::s.exchange("a")
    fun update() { this::i.update { it * 2 } }
}

// Property references, AtomicInt & co. and boxing are not used.
// 0 KMutableProperty
// 0 kotlin/jvm/internal/Reflection
// 0 PropertyReference
// 0 kotlin/concurrent/atomics/Atomic
// 0 java/util/concurrent/atomic
// 0 valueOf

// One VarHandle per property used in read-modify-write operations, initialized in <clinit>.
// 1 static synthetic Ljava/lang/invoke/VarHandle; i\$VH
// 1 static synthetic Ljava/lang/invoke/VarHandle; l\$VH
// 1 static synthetic Ljava/lang/invoke/VarHandle; s\$VH
// 3 INVOKEVIRTUAL java/lang/invoke/MethodHandles\$Lookup.unreflectVarHandle

// Exact (unboxed) signature polymorphic calls.
// 2 INVOKEVIRTUAL java/lang/invoke/VarHandle.compareAndSet \(LA;II\)Z
// 1 INVOKEVIRTUAL java/lang/invoke/VarHandle.getAndAdd \(LA;J\)J
// 1 INVOKEVIRTUAL java/lang/invoke/VarHandle.getAndSet \(LA;Ljava/lang/String;\)Ljava/lang/String;
