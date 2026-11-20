// STDLIB_JDK8
// FULL_JDK
// LIBRARY_PLATFORMS: JVM
package test

abstract class BoundedCollection<E : CharSequence> : Collection<E>

abstract class BoundedList<E : CharSequence> : List<E>

abstract class TransitivelyBoundedList<T : CharSequence, E : T> : List<E>

abstract class AnyBoundedList<E : Any> : List<E>

abstract class BoundedListOverrides<E : CharSequence> : List<E> {
    override fun contains(element: E): Boolean = false
    override fun indexOf(element: E): Int = 0
}

abstract class BoundedMutableListOverrides<E : CharSequence> : MutableList<E> {
    override fun remove(element: E): Boolean = false
}

abstract class BoundedMapOverrides<K : CharSequence, V : Number> : Map<K, V> {
    override fun get(key: K): V? = null
    override fun containsValue(value: V): Boolean = false
    override fun getOrDefault(key: K, defaultValue: V): V = defaultValue
}

abstract class BoundedMutableMapOverrides<K : CharSequence, V> : MutableMap<K, V> {
    override fun remove(key: K): V? = null
    override fun remove(key: K, value: V): Boolean = false
}

// LIGHT_ELEMENTS_NO_DECLARATION: AnyBoundedList.class[add;add;addAll;addAll;clear;getSize;listIterator;listIterator;remove;remove;removeAll;replaceAll;retainAll;set;size;sort;subList;toArray;toArray], BoundedCollection.class[add;addAll;clear;contains;contains;getSize;iterator;remove;removeAll;removeIf;retainAll;size;toArray;toArray], BoundedList.class[add;add;addAll;addAll;clear;contains;contains;getSize;indexOf;indexOf;lastIndexOf;lastIndexOf;listIterator;listIterator;remove;remove;removeAll;replaceAll;retainAll;set;size;sort;subList;toArray;toArray], BoundedListOverrides.class[add;add;addAll;addAll;clear;getSize;lastIndexOf;lastIndexOf;listIterator;listIterator;remove;remove;removeAll;replaceAll;retainAll;set;size;sort;subList;toArray;toArray], BoundedMapOverrides.class[clear;compute;computeIfAbsent;computeIfPresent;containsKey;containsKey;entrySet;getEntries;getKeys;getSize;getValues;keySet;merge;put;putAll;putIfAbsent;remove;remove;replace;replace;replaceAll;size;values], BoundedMutableListOverrides.class[contains;contains;getSize;indexOf;indexOf;lastIndexOf;lastIndexOf;remove;removeAt;size;toArray;toArray], BoundedMutableMapOverrides.class[containsKey;containsKey;entrySet;get;get;getEntries;getKeys;getSize;getValues;keySet;size;values], TransitivelyBoundedList.class[add;add;addAll;addAll;clear;contains;contains;getSize;indexOf;indexOf;lastIndexOf;lastIndexOf;listIterator;listIterator;remove;remove;removeAll;replaceAll;retainAll;set;size;sort;subList;toArray;toArray]
