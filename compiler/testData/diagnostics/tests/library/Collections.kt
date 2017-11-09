package collections

fun <T> testCollection(c: Collection<T>, t: T) {
    c.size
    c.isEmpty()
    c.contains(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
    val <!UNUSED_VARIABLE!>iterator<!>: Iterator<T> = c.iterator()
    c.containsAll(c)

    val <!UNUSED_VARIABLE!>mutableIterator<!>: MutableIterator<T> = <!TYPE_MISMATCH!>c.iterator()<!>
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
    c.contains(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
    val <!UNUSED_VARIABLE!>iterator<!>: Iterator<T> = c.iterator()
    c.containsAll(c)


    val <!UNUSED_VARIABLE!>mutableIterator<!>: MutableIterator<T> = c.iterator()
    c.add(t)
    c.remove(1 <!UNCHECKED_CAST!>as T<!>)
    c.addAll(c)
    c.removeAll(c)
    c.retainAll(c)
    c.clear()
}

fun <T> testList(l: List<T>, <!UNUSED_PARAMETER!>t<!>: T) {
    val <!NAME_SHADOWING!>t<!>: T = l.get(1)
    val <!UNUSED_VARIABLE!>i<!>: Int = l.indexOf(t)
    val <!UNUSED_VARIABLE!>i1<!>: Int = l.lastIndexOf(t)
    val <!UNUSED_VARIABLE!>listIterator<!>: ListIterator<T> = l.listIterator()
    val <!UNUSED_VARIABLE!>listIterator1<!>: ListIterator<T> = l.listIterator(1)
    val <!UNUSED_VARIABLE!>list<!>: List<T> = l.subList(1, 2)

    val <!UNUSED_VARIABLE!>value<!>: T = l.<!UNRESOLVED_REFERENCE!>set<!>(1, t)
    l.<!UNRESOLVED_REFERENCE!>add<!>(1, t)
    l.<!UNRESOLVED_REFERENCE!>remove<!>(1)
    val <!UNUSED_VARIABLE!>mutableListIterator<!>: MutableListIterator<T> = <!TYPE_MISMATCH!>l.listIterator()<!>
    val <!UNUSED_VARIABLE!>mutableListIterator1<!>: MutableListIterator<T> = <!TYPE_MISMATCH!>l.listIterator(1)<!>
    val <!UNUSED_VARIABLE!>mutableList<!>: MutableList<T> = <!TYPE_MISMATCH!>l.subList(1, 2)<!>
}

fun <T> testMutableList(l: MutableList<T>, t: T) {
    val <!UNUSED_VARIABLE!>value<!>: T = l.set(1, t)
    l.add(1, t)
    l.removeAt(1)
    val <!UNUSED_VARIABLE!>mutableListIterator<!>: MutableListIterator<T> = l.listIterator()
    val <!UNUSED_VARIABLE!>mutableListIterator1<!>: MutableListIterator<T> = l.listIterator(1)
    val <!UNUSED_VARIABLE!>mutableList<!>: MutableList<T> = l.subList(1, 2)
}

fun <T> testSet(s: Set<T>, t: T) {
    s.size
    s.isEmpty()
    s.contains(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
    val <!UNUSED_VARIABLE!>iterator<!>: Iterator<T> = s.iterator()
    s.containsAll(s)

    val <!UNUSED_VARIABLE!>mutableIterator<!>: MutableIterator<T> = <!TYPE_MISMATCH!>s.iterator()<!>
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
    s.contains(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
    val <!UNUSED_VARIABLE!>iterator<!>: Iterator<T> = s.iterator()
    s.containsAll(s)


    val <!UNUSED_VARIABLE!>mutableIterator<!>: MutableIterator<T> = s.iterator()
    s.add(t)
    s.remove(1 <!UNCHECKED_CAST!>as T<!>)
    s.addAll(s)
    s.removeAll(s)
    s.retainAll(s)
    s.clear()
}

fun <K, V> testMap(m: Map<K, V>) {
    val <!UNUSED_VARIABLE!>set<!>: Set<K> = m.keys
    val <!UNUSED_VARIABLE!>collection<!>: Collection<V> = m.values
    val <!UNUSED_VARIABLE!>set1<!>: Set<Map.Entry<K, V>> = m.entries

    val <!UNUSED_VARIABLE!>mutableSet<!>: MutableSet<K> = <!TYPE_MISMATCH!>m.keys<!>
    val <!UNUSED_VARIABLE!>mutableCollection<!>: MutableCollection<V> = <!TYPE_MISMATCH!>m.values<!>
    val <!UNUSED_VARIABLE!>mutableSet1<!>: MutableSet<MutableMap.MutableEntry<K, V>> = <!TYPE_MISMATCH!>m.entries<!>
}

fun <K, V> testMutableMap(m: MutableMap<K, V>) {
    val <!UNUSED_VARIABLE!>mutableSet<!>: MutableSet<K> = m.keys
    val <!UNUSED_VARIABLE!>mutableCollection<!>: MutableCollection<V> = m.values
    val <!UNUSED_VARIABLE!>mutableSet1<!>: MutableSet<MutableMap.MutableEntry<K, V>> = m.entries
}

fun <T> array(vararg <!UNUSED_PARAMETER!>t<!>: T): Array<T> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>