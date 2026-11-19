// JVM_TARGET: 1.8
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

// Property references, AtomicInt & co., VarHandles and boxing are not used.
// 0 KMutableProperty
// 0 kotlin/jvm/internal/Reflection
// 0 PropertyReference
// 0 kotlin/concurrent/atomics/Atomic
// 0 java/lang/invoke
// 0 valueOf

// One field updater per property used in read-modify-write operations, initialized in <clinit>.
// 1 static synthetic Ljava/util/concurrent/atomic/AtomicIntegerFieldUpdater; i\$FU
// 1 static synthetic Ljava/util/concurrent/atomic/AtomicLongFieldUpdater; l\$FU
// 1 static synthetic Ljava/util/concurrent/atomic/AtomicReferenceFieldUpdater; s\$FU
// 1 INVOKESTATIC java/util/concurrent/atomic/AtomicIntegerFieldUpdater.newUpdater \(Ljava/lang/Class;Ljava/lang/String;\)
// 1 INVOKESTATIC java/util/concurrent/atomic/AtomicLongFieldUpdater.newUpdater \(Ljava/lang/Class;Ljava/lang/String;\)
// 1 INVOKESTATIC java/util/concurrent/atomic/AtomicReferenceFieldUpdater.newUpdater \(Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/String;\)
// 1 INVOKEVIRTUAL java/lang/reflect/Field.getType

// Unboxed calls.
// 2 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicIntegerFieldUpdater.compareAndSet \(Ljava/lang/Object;II\)Z
// 1 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicLongFieldUpdater.addAndGet \(Ljava/lang/Object;J\)J
// 1 INVOKEVIRTUAL java/util/concurrent/atomic/AtomicReferenceFieldUpdater.getAndSet \(Ljava/lang/Object;Ljava/lang/Object;\)Ljava/lang/Object;
