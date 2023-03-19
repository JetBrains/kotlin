// IGNORE_BACKEND_K2: JVM_IR
// FIR status: KT-57268 K2: extra methods `remove` and/or `getOrDefault` are generated for Map subclasses with JDK 1.6 in dependencies

class DStringIterator(d: Iterator<String>) : Iterator<String> by d

class DStringListIterator(d: ListIterator<String>) : ListIterator<String> by d

class DStringIterable(d: Iterable<String>) : Iterable<String> by d

class DStringCollection(d: Collection<String>) : Collection<String> by d

class DStringSet(d: Set<String>) : Set<String> by d

class DStringList(d: List<String>) : List<String> by d

class DStringMap(d: Map<String, Number>) : Map<String, Number> by d

class DStringMapEntry(d: Map.Entry<String, Number>) : Map.Entry<String, Number> by d

class DStringCollectionBySet(d: Set<String>) : Collection<String> by d

class DStringCollectionByList(d: List<String>) : Collection<String> by d
