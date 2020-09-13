// FULL_JDK
// TARGET_BACKEND: JVM
// WITH_RUNTIME

// FILE: Bag.java

import java.util.*;
import java.util.function.Supplier;

public class Bag<K, V, C extends E, E extends Collection<V>> {
    public Bag(Map<K, C> backingMap, Supplier<? extends C> innerCollectionCreator) {
        // TODO
    }

    public final void add(K key, V value) {
        //TODO
    }

    public static class ListBag<K, V> extends Bag<K, V, List<V>, List<V>> {
        public ListBag(Map<K, List<V>> backingMap, Supplier<? extends List<V>> innerCollectionCreator) {
            super(backingMap, innerCollectionCreator);
        }
    }
}

// FILE: test.kt

import java.util.*

fun <K, V, C : E, E : Collection<V>, B : Bag<K, V, C, E>> Iterable<V>.groupByTo(
    destination: B,
    keySelector: (V) -> K
): B = TODO()

enum class Format { Foo, Bar }
class Instance(val format: Format)

fun test(allInstances: List<Instance>, e: EnumMap<Format, List<Instance>>) {
    val doesntWork = allInstances.groupByTo(
        Bag.ListBag(e, ::LinkedList)
    ) { it.format }
}

fun box(): String {
    return "OK"
}