import kotlin.collections.*

@CompileTimeCalculation
fun testAdd(mutableList: MutableList<Int>, newElem: Int): String {
    mutableList.add(newElem)
    return "After add new size is " + mutableList.size
}

@CompileTimeCalculation
fun <T> testRemove(mutableList: MutableList<T>, toRemove: T): String {
    mutableList.remove(toRemove)
    return "After remove new size is " + mutableList.size
}

@CompileTimeCalculation
fun testAddAll(mutableList: MutableList<Double>, toAdd: List<Double>): String {
    mutableList.addAll(toAdd)
    return "After addAll new size is " + mutableList.size
}

@CompileTimeCalculation
fun testIterator(mutableList: MutableList<Byte>): String {
    var sum = 0
    for (byte in mutableList) {
        sum += byte
    }
    return "Sum = " + sum
}

const val emptyMutableListSize = <!EVALUATED: `0`!>mutableListOf<Any>().size<!>
const val mutableListSize = <!EVALUATED: `3`!>mutableListOf(1, 2, 3).size<!>
const val mutableListAdd = <!EVALUATED: `After add new size is 4`!>testAdd(mutableListOf(1, 2, 3), 4)<!>
const val mutableListRemove1 = <!EVALUATED: `After remove new size is 2`!>testRemove(mutableListOf("1", "2", "3"), "1")<!>
const val mutableListRemove2 = <!EVALUATED: `After remove new size is 3`!>testRemove(mutableListOf("1", "2", "3"), "4")<!>
const val mutableListAddAll = <!EVALUATED: `After addAll new size is 5`!>testAddAll(mutableListOf(1.0, 2.0, 3.0), listOf(4.333, -5.5))<!>
const val mutableListSum = <!EVALUATED: `Sum = 136`!>testIterator(mutableListOf<Byte>(1, (-2).toByte(), 127, 10, 0))<!>
const val mutableListSubList = <!EVALUATED: `2`!>mutableListOf(1, 2, 3, 4).subList(0, 2).size<!>
