// FULL_JDK

// IGNORE_BACKEND_K2: JVM_IR
// FIR status: KT-57269 K2: collection stub for `sort` is not generated for custom List subclasses

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
