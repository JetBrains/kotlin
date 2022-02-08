import kotlin.collections.*

@CompileTimeCalculation
open class MyArrayList<E>: ArrayList<E>() {
    var addCounter = 0
    override fun add(element: E): Boolean {
        addCounter++
        return super.add(element)
    }
}

@CompileTimeCalculation
class MyOtherArrayList<E>: MyArrayList<E>() {
    override fun addAll(elements: Collection<E>): Boolean {
        return true // do nothing
    }
}

@CompileTimeCalculation
fun test(list: MyArrayList<Int>): String {
    list.add(1)
    list.add(2)
    list.add(3)

    val otherList = arrayListOf(4, 5, 6)
    list.addAll(otherList)
    list.addAll(emptyList<Int>())
    return "Counter " + list.addCounter + "; size " + list.size
}

const val testResult1 = <!EVALUATED: `Counter 3; size 6`!>test(MyArrayList<Int>())<!>
const val testResult2 = <!EVALUATED: `Counter 3; size 3`!>test(MyOtherArrayList<Int>())<!>
