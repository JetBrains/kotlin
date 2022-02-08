@CompileTimeCalculation
interface LocalObject {
    fun getNum(): Int
}

@CompileTimeCalculation
fun getLocalObject(num: Int) = object : LocalObject {
    override fun getNum() = num
}

@CompileTimeCalculation
class A(val a: Int) {
    val localObject = object : LocalObject {
        override fun getNum() = a
    }
}

const val a = getLocalObject(10).<!EVALUATED: `10`!>getNum()<!>
const val b = A(2).localObject.<!EVALUATED: `2`!>getNum()<!>
