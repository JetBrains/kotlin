package collections

fun <T> testCollection(c: Collection<T>, t: T) {
    c.size
    c.isEmpty()
    c.contains(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
    val iterator: Iterator<T> = c.iterator()
    c.containsAll(c)

    val mutableIterator: MutableIterator<T> = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>c.iterator()<!>
    c.<!UNRESOLVED_REFERENCE!>add<!>(t)
    c.<!UNRESOLVED_REFERENCE!>remove<!>(1)
    c.<!UNRESOLVED_REFERENCE!>addAll<!>(c)
    c.<!UNRESOLVED_REFERENCE!>removeAll<!>(c)
    c.<!UNRESOLVED_REFERENCE!>retainAll<!>(c)
    c.<!UNRESOLVED_REFERENCE!>clear<!>()

}
fun <T> testMutableCollection(c: MutableCollection<T>, t: T) {
    c.size
    c.isEmpty()
    c.contains(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
    val iterator: Iterator<T> = c.iterator()
    c.containsAll(c)


    val mutableIterator: MutableIterator<T> = c.iterator()
    c.add(t)
    c.remove(1 as T)
    c.addAll(c)
    c.removeAll(c)
    c.retainAll(c)
    c.clear()
}

fun <T> testList(l: List<T>, t: T) {
    val t: T = l.get(1)
    val i: Int = l.indexOf(t)
    val i1: Int = l.lastIndexOf(t)
    val listIterator: ListIterator<T> = l.listIterator()
    val listIterator1: ListIterator<T> = l.listIterator(1)
    val list: List<T> = l.subList(1, 2)

    val value: T = l.<!UNRESOLVED_REFERENCE!>set<!>(1, t)
    l.<!UNRESOLVED_REFERENCE!>add<!>(1, t)
    l.<!UNRESOLVED_REFERENCE!>remove<!>(1)
    val mutableListIterator: MutableListIterator<T> = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>l.listIterator()<!>
    val mutableListIterator1: MutableListIterator<T> = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>l.listIterator(1)<!>
    val mutableList: MutableList<T> = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>l.subList(1, 2)<!>
}

fun <T> testMutableList(l: MutableList<T>, t: T) {
    val value: T = l.set(1, t)
    l.add(1, t)
    l.removeAt(1)
    val mutableListIterator: MutableListIterator<T> = l.listIterator()
    val mutableListIterator1: MutableListIterator<T> = l.listIterator(1)
    val mutableList: MutableList<T> = l.subList(1, 2)
}

fun <T> testSet(s: Set<T>, t: T) {
    s.size
    s.isEmpty()
    s.contains(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
    val iterator: Iterator<T> = s.iterator()
    s.containsAll(s)

    val mutableIterator: MutableIterator<T> = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>s.iterator()<!>
    s.<!UNRESOLVED_REFERENCE!>add<!>(t)
    s.<!UNRESOLVED_REFERENCE!>remove<!>(1)
    s.<!UNRESOLVED_REFERENCE!>addAll<!>(s)
    s.<!UNRESOLVED_REFERENCE!>removeAll<!>(s)
    s.<!UNRESOLVED_REFERENCE!>retainAll<!>(s)
    s.<!UNRESOLVED_REFERENCE!>clear<!>()

}
fun <T> testMutableSet(s: MutableSet<T>, t: T) {
    s.size
    s.isEmpty()
    s.contains(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
    val iterator: Iterator<T> = s.iterator()
    s.containsAll(s)


    val mutableIterator: MutableIterator<T> = s.iterator()
    s.add(t)
    s.remove(1 as T)
    s.addAll(s)
    s.removeAll(s)
    s.retainAll(s)
    s.clear()
}

fun <K, V> testMap(m: Map<K, V>) {
    val set: Set<K> = m.keys
    val collection: Collection<V> = m.values
    val set1: Set<Map.Entry<K, V>> = m.entries

    val mutableSet: MutableSet<K> = <!INITIALIZER_TYPE_MISMATCH!>m.keys<!>
    val mutableCollection: MutableCollection<V> = <!INITIALIZER_TYPE_MISMATCH!>m.values<!>
    val mutableSet1: MutableSet<MutableMap.MutableEntry<K, V>> = <!INITIALIZER_TYPE_MISMATCH!>m.entries<!>
}

fun <K, V> testMutableMap(m: MutableMap<K, V>) {
    val mutableSet: MutableSet<K> = m.keys
    val mutableCollection: MutableCollection<V> = m.values
    val mutableSet1: MutableSet<MutableMap.MutableEntry<K, V>> = m.entries
}

fun <T> array(vararg t: T): Array<T> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
