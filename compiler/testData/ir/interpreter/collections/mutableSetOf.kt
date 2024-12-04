import kotlin.collections.*

@CompileTimeCalculation
fun testAdd(mutableSet: MutableSet<Int>, newElem: Int): String {
    mutableSet.add(newElem)
    return "After add new size is " + mutableSet.size
}

@CompileTimeCalculation
fun <T> testRemove(mutableSet: MutableSet<T>, toRemove: T): String {
    mutableSet.remove(toRemove)
    return "After remove new size is " + mutableSet.size
}

@CompileTimeCalculation
fun testAddAll(mutableSet: MutableSet<Double>, toAdd: Set<Double>): String {
    mutableSet.addAll(toAdd)
    return "After addAll new size is " + mutableSet.size
}

@CompileTimeCalculation
fun testIterator(mutableSet: MutableSet<Byte>): String {
    var sum = 0
    for (byte in mutableSet) {
        sum += byte
    }
    return "Sum = " + sum
}

const val emptyMutableSetSize = <!EVALUATED: `0`!>mutableSetOf<Any>().size<!>
const val mutableSetSize = <!EVALUATED: `3`!>mutableSetOf(1, 2, 3).size<!>
const val mutableSetAdd = <!EVALUATED: `After add new size is 4`!>testAdd(mutableSetOf(1, 2, 3), 4)<!>
const val mutableSetRemove1 = <!EVALUATED: `After remove new size is 2`!>testRemove(mutableSetOf("1", "2", "3"), "1")<!>
const val mutableSetRemove2 = <!EVALUATED: `After remove new size is 3`!>testRemove(mutableSetOf("1", "2", "3"), "4")<!>
const val mutableSetAddAll = <!EVALUATED: `After addAll new size is 5`!>testAddAll(mutableSetOf(1.0, 2.0, 3.0), setOf(4.333, -5.5))<!>
const val mutableSetSum = <!EVALUATED: `Sum = 136`!>testIterator(mutableSetOf<Byte>(1, (-2).toByte(), 127, 10, 0))<!>
