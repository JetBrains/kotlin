//class MyIterable : Test.IterableImpl()
//class MyIterator : Test.IteratorImpl()
class MyMapEntry : Test.MapEntryImpl()

fun box(): String {

    val b = MyMapEntry()
    b.getKey()
    b.getValue()
    b.setValue(null)

    return "OK"
}
