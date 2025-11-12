// IntelliJ API Decompiler stub source generated from a class file
// Implementation of methods is not available

package kotlin.collections

public abstract class BooleanIterator public constructor() : kotlin.collections.Iterator<kotlin.Boolean> {
    public final operator fun next(): kotlin.Boolean { /* compiled code */ }

    public abstract fun nextBoolean(): kotlin.Boolean
}

public abstract class ByteIterator public constructor() : kotlin.collections.Iterator<kotlin.Byte> {
    public final operator fun next(): kotlin.Byte { /* compiled code */ }

    public abstract fun nextByte(): kotlin.Byte
}

public abstract class CharIterator public constructor() : kotlin.collections.Iterator<kotlin.Char> {
    public final operator fun next(): kotlin.Char { /* compiled code */ }

    public abstract fun nextChar(): kotlin.Char
}

public interface Collection<out E> : kotlin.collections.Iterable<E> {
    public abstract val size: kotlin.Int

    public abstract fun isEmpty(): kotlin.Boolean

    public abstract operator fun contains(element: E): kotlin.Boolean

    public abstract operator fun iterator(): kotlin.collections.Iterator<E>

    public abstract fun containsAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean
}

public abstract class DoubleIterator public constructor() : kotlin.collections.Iterator<kotlin.Double> {
    public final operator fun next(): kotlin.Double { /* compiled code */ }

    public abstract fun nextDouble(): kotlin.Double
}

public abstract class FloatIterator public constructor() : kotlin.collections.Iterator<kotlin.Float> {
    public final operator fun next(): kotlin.Float { /* compiled code */ }

    public abstract fun nextFloat(): kotlin.Float
}

public abstract class IntIterator public constructor() : kotlin.collections.Iterator<kotlin.Int> {
    public final operator fun next(): kotlin.Int { /* compiled code */ }

    public abstract fun nextInt(): kotlin.Int
}

public interface Iterable<out T> {
    public abstract operator fun iterator(): kotlin.collections.Iterator<T>
}

public interface Iterator<out T> {
    public abstract operator fun next(): T

    public abstract operator fun hasNext(): kotlin.Boolean
}

public interface List<out E> : kotlin.collections.Collection<E> {
    @kotlin.ExperimentalStdlibApi public companion object {
        @kotlin.ExperimentalStdlibApi public final operator fun <T> of(): kotlin.collections.List<T> { /* compiled code */ }

        @kotlin.ExperimentalStdlibApi public final operator fun <T> of(element: T): kotlin.collections.List<T> { /* compiled code */ }

        @kotlin.ExperimentalStdlibApi public final operator fun <T> of(vararg elements: T): kotlin.collections.List<T> { /* compiled code */ }
    }

    public abstract val size: kotlin.Int

    public abstract fun isEmpty(): kotlin.Boolean

    public abstract operator fun contains(element: E): kotlin.Boolean

    public abstract operator fun iterator(): kotlin.collections.Iterator<E>

    public abstract fun containsAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    public abstract operator fun get(index: kotlin.Int): E

    public abstract fun indexOf(element: E): kotlin.Int

    public abstract fun lastIndexOf(element: E): kotlin.Int

    public abstract fun listIterator(): kotlin.collections.ListIterator<E>

    public abstract fun listIterator(index: kotlin.Int): kotlin.collections.ListIterator<E>

    public abstract fun subList(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.collections.List<E>
}

public interface ListIterator<out T> : kotlin.collections.Iterator<T> {
    public abstract operator fun next(): T

    public abstract operator fun hasNext(): kotlin.Boolean

    public abstract fun hasPrevious(): kotlin.Boolean

    public abstract fun previous(): T

    public abstract fun nextIndex(): kotlin.Int

    public abstract fun previousIndex(): kotlin.Int
}

public abstract class LongIterator public constructor() : kotlin.collections.Iterator<kotlin.Long> {
    public final operator fun next(): kotlin.Long { /* compiled code */ }

    public abstract fun nextLong(): kotlin.Long
}

public interface Map<K, out V> {
    public abstract val size: kotlin.Int

    public abstract val keys: kotlin.collections.Set<K>

    public abstract val values: kotlin.collections.Collection<V>

    public abstract val entries: kotlin.collections.Set<kotlin.collections.Map.Entry<K, V>>

    public abstract fun isEmpty(): kotlin.Boolean

    public abstract fun containsKey(key: K): kotlin.Boolean

    public abstract fun containsValue(value: V): kotlin.Boolean

    public abstract operator fun get(key: K): V?

    @kotlin.SinceKotlin @kotlin.internal.PlatformDependent public open fun getOrDefault(key: K, defaultValue: V): V { /* compiled code */ }

    public interface Entry<out K, out V> {
        public abstract val key: K

        public abstract val value: V
    }
}

public interface MutableCollection<E> : kotlin.collections.Collection<E>, kotlin.collections.MutableIterable<E> {
    public abstract operator fun iterator(): kotlin.collections.MutableIterator<E>

    @kotlin.IgnorableReturnValue public abstract fun add(element: E): kotlin.Boolean

    @kotlin.IgnorableReturnValue public abstract fun remove(element: E): kotlin.Boolean

    @kotlin.IgnorableReturnValue public abstract fun addAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    @kotlin.IgnorableReturnValue public abstract fun removeAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    @kotlin.IgnorableReturnValue public abstract fun retainAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    public abstract fun clear(): kotlin.Unit
}

public interface MutableIterable<out T> : kotlin.collections.Iterable<T> {
    public abstract operator fun iterator(): kotlin.collections.MutableIterator<T>
}

public interface MutableIterator<out T> : kotlin.collections.Iterator<T> {
    public abstract fun remove(): kotlin.Unit
}

public interface MutableList<E> : kotlin.collections.List<E>, kotlin.collections.MutableCollection<E> {
    @kotlin.ExperimentalStdlibApi public companion object {
        @kotlin.ExperimentalStdlibApi public final operator fun <T> of(): kotlin.collections.MutableList<T> { /* compiled code */ }

        @kotlin.ExperimentalStdlibApi public final operator fun <T> of(element: T): kotlin.collections.MutableList<T> { /* compiled code */ }

        @kotlin.ExperimentalStdlibApi public final operator fun <T> of(vararg elements: T): kotlin.collections.MutableList<T> { /* compiled code */ }
    }

    @kotlin.IgnorableReturnValue public abstract fun add(element: E): kotlin.Boolean

    @kotlin.IgnorableReturnValue public abstract fun remove(element: E): kotlin.Boolean

    @kotlin.IgnorableReturnValue public abstract fun addAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    @kotlin.IgnorableReturnValue public abstract fun addAll(index: kotlin.Int, elements: kotlin.collections.Collection<E>): kotlin.Boolean

    @kotlin.IgnorableReturnValue public abstract fun removeAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    @kotlin.IgnorableReturnValue public abstract fun retainAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    public abstract fun clear(): kotlin.Unit

    @kotlin.IgnorableReturnValue public abstract operator fun set(index: kotlin.Int, element: E): E

    public abstract fun add(index: kotlin.Int, element: E): kotlin.Unit

    @kotlin.IgnorableReturnValue public abstract fun removeAt(index: kotlin.Int): E

    public abstract fun listIterator(): kotlin.collections.MutableListIterator<E>

    public abstract fun listIterator(index: kotlin.Int): kotlin.collections.MutableListIterator<E>

    public abstract fun subList(fromIndex: kotlin.Int, toIndex: kotlin.Int): kotlin.collections.MutableList<E>
}

public interface MutableListIterator<T> : kotlin.collections.ListIterator<T>, kotlin.collections.MutableIterator<T> {
    public abstract operator fun next(): T

    public abstract operator fun hasNext(): kotlin.Boolean

    public abstract fun remove(): kotlin.Unit

    public abstract fun set(element: T): kotlin.Unit

    public abstract fun add(element: T): kotlin.Unit
}

public interface MutableMap<K, V> : kotlin.collections.Map<K, V> {
    public abstract val keys: kotlin.collections.MutableSet<K>

    public abstract val values: kotlin.collections.MutableCollection<V>

    public abstract val entries: kotlin.collections.MutableSet<kotlin.collections.MutableMap.MutableEntry<K, V>>

    @kotlin.IgnorableReturnValue public abstract fun put(key: K, value: V): V?

    @kotlin.IgnorableReturnValue public abstract fun remove(key: K): V?

    @kotlin.SinceKotlin @kotlin.internal.PlatformDependent public open fun remove(key: K, value: V): kotlin.Boolean { /* compiled code */ }

    public abstract fun putAll(from: kotlin.collections.Map<out K, V>): kotlin.Unit

    public abstract fun clear(): kotlin.Unit

    public interface MutableEntry<K, V> : kotlin.collections.Map.Entry<K, V> {
        @kotlin.IgnorableReturnValue public abstract fun setValue(newValue: V): V
    }
}

public interface MutableSet<E> : kotlin.collections.Set<E>, kotlin.collections.MutableCollection<E> {
    @kotlin.ExperimentalStdlibApi public companion object {
        @kotlin.ExperimentalStdlibApi public final operator fun <T> of(): kotlin.collections.MutableSet<T> { /* compiled code */ }

        @kotlin.ExperimentalStdlibApi public final operator fun <T> of(element: T): kotlin.collections.MutableSet<T> { /* compiled code */ }

        @kotlin.ExperimentalStdlibApi public final operator fun <T> of(vararg elements: T): kotlin.collections.MutableSet<T> { /* compiled code */ }
    }

    public abstract operator fun iterator(): kotlin.collections.MutableIterator<E>

    @kotlin.IgnorableReturnValue public abstract fun add(element: E): kotlin.Boolean

    @kotlin.IgnorableReturnValue public abstract fun remove(element: E): kotlin.Boolean

    @kotlin.IgnorableReturnValue public abstract fun addAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    @kotlin.IgnorableReturnValue public abstract fun removeAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    @kotlin.IgnorableReturnValue public abstract fun retainAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean

    public abstract fun clear(): kotlin.Unit
}

public interface Set<out E> : kotlin.collections.Collection<E> {
    @kotlin.ExperimentalStdlibApi public companion object {
        @kotlin.ExperimentalStdlibApi public final operator fun <T> of(): kotlin.collections.Set<T> { /* compiled code */ }

        @kotlin.ExperimentalStdlibApi public final operator fun <T> of(element: T): kotlin.collections.Set<T> { /* compiled code */ }

        @kotlin.ExperimentalStdlibApi public final operator fun <T> of(vararg elements: T): kotlin.collections.Set<T> { /* compiled code */ }
    }

    public abstract val size: kotlin.Int

    public abstract fun isEmpty(): kotlin.Boolean

    public abstract operator fun contains(element: E): kotlin.Boolean

    public abstract operator fun iterator(): kotlin.collections.Iterator<E>

    public abstract fun containsAll(elements: kotlin.collections.Collection<E>): kotlin.Boolean
}

public abstract class ShortIterator public constructor() : kotlin.collections.Iterator<kotlin.Short> {
    public final operator fun next(): kotlin.Short { /* compiled code */ }

    public abstract fun nextShort(): kotlin.Short
}
