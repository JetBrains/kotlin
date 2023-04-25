// IGNORE_BACKEND_K2: JVM_IR
// FIR status: KT-57268 K2: extra methods `remove` and/or `getOrDefault` are generated for Map subclasses with JDK 1.6 in dependencies

class DGenericIterator<T>(d: Iterator<T>) : Iterator<T> by d

class DGenericListIterator<T>(d: ListIterator<T>) : ListIterator<T> by d

class DGenericIterable<T>(d: Iterable<T>) : Iterable<T> by d

class DGenericCollection<T>(d: Collection<T>) : Collection<T> by d

class DGenericSet<T>(d: Set<T>) : Set<T> by d

class DGenericList<T>(d: List<T>) : List<T> by d

class DGenericMap<K, V>(d: Map<K, V>) : Map<K, V> by d

class DGenericMapEntry<K, V>(d: Map.Entry<K, V>) : Map.Entry<K, V> by d

class DGenericCollectionBySet<T>(d: Set<T>) : Collection<T> by d

class DGenericCollectionByList<T>(d: List<T>) : Collection<T> by d
