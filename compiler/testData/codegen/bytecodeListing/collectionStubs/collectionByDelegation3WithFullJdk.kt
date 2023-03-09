// FULL_JDK
// See also: https://youtrack.jetbrains.com/issue/KT-42330

// IGNORE_BACKEND_K2: JVM_IR
// FIR status: KT-57269 K2: collection stub for `sort` is not generated for custom List subclasses

class DIntIterator(d: Iterator<Int>) : Iterator<Int> by d

class DIntListIterator(d: ListIterator<Int>) : ListIterator<Int> by d

class DIntIterable(d: Iterable<Int>) : Iterable<Int> by d

class DIntCollection(d: Collection<Int>) : Collection<Int> by d

class DIntSet(d: Set<Int>) : Set<Int> by d

class DIntList(d: List<Int>) : List<Int> by d

class DIntMap(d: Map<Int, Double>) : Map<Int, Double> by d

class DIntMapEntry(d: Map.Entry<Int, Double>) : Map.Entry<Int, Double> by d

class DIntCollectionBySet(d: Set<Int>) : Collection<Int> by d

class DIntCollectionByList(d: List<Int>) : Collection<Int> by d
