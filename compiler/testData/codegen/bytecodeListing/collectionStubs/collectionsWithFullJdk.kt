// FULL_JDK
// See: KT-42114, KT-42115

// IGNORE_BACKEND_K2: JVM_IR
// FIR status: KT-57269 K2: collection stub for `sort` is not generated for custom List subclasses

abstract class AbstractIterator : Iterator<String>

abstract class AbstractIterable : Iterable<String>

abstract class AbstractCollection : Collection<String>

abstract class AbstractSet : Set<String>

abstract class AbstractList : List<String>

abstract class AbstractListIterator : ListIterator<String>

abstract class AbstractMap : Map<String, Number>

abstract class AbstractMapEntry : Map.Entry<String, Number>
