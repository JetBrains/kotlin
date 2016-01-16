//class MyIterable : Test.IterableImpl()
//class MyIterator : Test.IteratorImpl()
class MyMapEntry : Test.MapEntryImpl()

fun box(): String {

    val b = MyMapEntry()
    b.key
    b.value
    b.setValue(null)

    return "OK"
}
