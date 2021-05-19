import kotlin.collections.*

@CompileTimeCalculation
class MyArrayList<E>: ArrayList<E>() {
    var addCounter = 0
    override fun add(element: E): Boolean {
        addCounter++
        return super.add(element)
    }
}

@CompileTimeCalculation
fun test(): String {
    val list = MyArrayList<Int>()
    list.add(1)
    list.add(2)
    list.add(3)
    return "Counter " + list.addCounter + "; size " + list.size
}

const val testResult = <!EVALUATED: `Counter 3; size 3`!>test()<!>
