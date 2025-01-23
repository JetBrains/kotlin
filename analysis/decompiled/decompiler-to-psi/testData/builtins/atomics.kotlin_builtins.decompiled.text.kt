// IntelliJ API Decompiler stub source generated from a class file
// Implementation of methods is not available

package kotlin.concurrent.atomics

@kotlin.SinceKotlin @kotlin.concurrent.atomics.ExperimentalAtomicApi public final class AtomicArray<T> {
    public constructor(array: kotlin.Array<T>) { /* compiled code */ }

    public final val size: kotlin.Int /* compiled code */

    public final fun loadAt(index: kotlin.Int): T { /* compiled code */ }

    public final fun storeAt(index: kotlin.Int, newValue: T): kotlin.Unit { /* compiled code */ }

    public final fun exchangeAt(index: kotlin.Int, newValue: T): T { /* compiled code */ }

    public final fun compareAndSetAt(index: kotlin.Int, expectedValue: T, newValue: T): kotlin.Boolean { /* compiled code */ }

    public final fun compareAndExchangeAt(index: kotlin.Int, expectedValue: T, newValue: T): T { /* compiled code */ }

    public open fun toString(): kotlin.String { /* compiled code */ }
}

@kotlin.SinceKotlin @kotlin.concurrent.atomics.ExperimentalAtomicApi public final class AtomicBoolean public constructor(value: kotlin.Boolean) {
    public final fun load(): kotlin.Boolean { /* compiled code */ }

    public final fun store(newValue: kotlin.Boolean): kotlin.Unit { /* compiled code */ }

    public final fun exchange(newValue: kotlin.Boolean): kotlin.Boolean { /* compiled code */ }

    public final fun compareAndSet(expectedValue: kotlin.Boolean, newValue: kotlin.Boolean): kotlin.Boolean { /* compiled code */ }

    public final fun compareAndExchange(expectedValue: kotlin.Boolean, newValue: kotlin.Boolean): kotlin.Boolean { /* compiled code */ }

    public open fun toString(): kotlin.String { /* compiled code */ }
}

@kotlin.SinceKotlin @kotlin.concurrent.atomics.ExperimentalAtomicApi public final class AtomicInt public constructor(value: kotlin.Int) {
    public final fun load(): kotlin.Int { /* compiled code */ }

    public final fun store(newValue: kotlin.Int): kotlin.Unit { /* compiled code */ }

    public final fun exchange(newValue: kotlin.Int): kotlin.Int { /* compiled code */ }

    public final fun compareAndSet(expectedValue: kotlin.Int, newValue: kotlin.Int): kotlin.Boolean { /* compiled code */ }

    public final fun compareAndExchange(expectedValue: kotlin.Int, newValue: kotlin.Int): kotlin.Int { /* compiled code */ }

    public final fun fetchAndAdd(delta: kotlin.Int): kotlin.Int { /* compiled code */ }

    public final fun addAndFetch(delta: kotlin.Int): kotlin.Int { /* compiled code */ }

    public open fun toString(): kotlin.String { /* compiled code */ }
}

@kotlin.SinceKotlin @kotlin.concurrent.atomics.ExperimentalAtomicApi public final class AtomicIntArray {
    public constructor(size: kotlin.Int) { /* compiled code */ }

    public constructor(array: kotlin.IntArray) { /* compiled code */ }

    public final val size: kotlin.Int /* compiled code */

    public final fun loadAt(index: kotlin.Int): kotlin.Int { /* compiled code */ }

    public final fun storeAt(index: kotlin.Int, newValue: kotlin.Int): kotlin.Unit { /* compiled code */ }

    public final fun exchangeAt(index: kotlin.Int, newValue: kotlin.Int): kotlin.Int { /* compiled code */ }

    public final fun compareAndSetAt(index: kotlin.Int, expectedValue: kotlin.Int, newValue: kotlin.Int): kotlin.Boolean { /* compiled code */ }

    public final fun compareAndExchangeAt(index: kotlin.Int, expectedValue: kotlin.Int, newValue: kotlin.Int): kotlin.Int { /* compiled code */ }

    public final fun fetchAndAddAt(index: kotlin.Int, delta: kotlin.Int): kotlin.Int { /* compiled code */ }

    public final fun addAndFetchAt(index: kotlin.Int, delta: kotlin.Int): kotlin.Int { /* compiled code */ }

    public open fun toString(): kotlin.String { /* compiled code */ }
}

@kotlin.SinceKotlin @kotlin.concurrent.atomics.ExperimentalAtomicApi public final class AtomicLong public constructor(value: kotlin.Long) {
    public final fun load(): kotlin.Long { /* compiled code */ }

    public final fun store(newValue: kotlin.Long): kotlin.Unit { /* compiled code */ }

    public final fun exchange(newValue: kotlin.Long): kotlin.Long { /* compiled code */ }

    public final fun compareAndSet(expectedValue: kotlin.Long, newValue: kotlin.Long): kotlin.Boolean { /* compiled code */ }

    public final fun compareAndExchange(expectedValue: kotlin.Long, newValue: kotlin.Long): kotlin.Long { /* compiled code */ }

    public final fun fetchAndAdd(delta: kotlin.Long): kotlin.Long { /* compiled code */ }

    public final fun addAndFetch(delta: kotlin.Long): kotlin.Long { /* compiled code */ }

    public open fun toString(): kotlin.String { /* compiled code */ }
}

@kotlin.SinceKotlin @kotlin.concurrent.atomics.ExperimentalAtomicApi public final class AtomicLongArray {
    public constructor(size: kotlin.Int) { /* compiled code */ }

    public constructor(array: kotlin.LongArray) { /* compiled code */ }

    public final val size: kotlin.Int /* compiled code */

    public final fun loadAt(index: kotlin.Int): kotlin.Long { /* compiled code */ }

    public final fun storeAt(index: kotlin.Int, newValue: kotlin.Long): kotlin.Unit { /* compiled code */ }

    public final fun exchangeAt(index: kotlin.Int, newValue: kotlin.Long): kotlin.Long { /* compiled code */ }

    public final fun compareAndSetAt(index: kotlin.Int, expectedValue: kotlin.Long, newValue: kotlin.Long): kotlin.Boolean { /* compiled code */ }

    public final fun compareAndExchangeAt(index: kotlin.Int, expectedValue: kotlin.Long, newValue: kotlin.Long): kotlin.Long { /* compiled code */ }

    public final fun fetchAndAddAt(index: kotlin.Int, delta: kotlin.Long): kotlin.Long { /* compiled code */ }

    public final fun addAndFetchAt(index: kotlin.Int, delta: kotlin.Long): kotlin.Long { /* compiled code */ }

    public open fun toString(): kotlin.String { /* compiled code */ }
}

@kotlin.SinceKotlin @kotlin.concurrent.atomics.ExperimentalAtomicApi public final class AtomicReference<T> public constructor(value: T) {
    public final fun load(): T { /* compiled code */ }

    public final fun store(newValue: T): kotlin.Unit { /* compiled code */ }

    public final fun exchange(newValue: T): T { /* compiled code */ }

    public final fun compareAndSet(expectedValue: T, newValue: T): kotlin.Boolean { /* compiled code */ }

    public final fun compareAndExchange(expectedValue: T, newValue: T): T { /* compiled code */ }

    public open fun toString(): kotlin.String { /* compiled code */ }
}

