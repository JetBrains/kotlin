class MyIterable : Test.IterableImpl()
class MyIterator : Test.IteratorImpl()
class MyMapEntry : Test.MapEntryImpl()

fun box(): String {
    MyIterable().iterator()

    val a = MyIterator()
    a.hasNext()
    a.next()
    a.remove()

    val b = MyMapEntry()
    b.getKey()
    b.getValue()
    b.setValue(null)

    return "OK"
}
