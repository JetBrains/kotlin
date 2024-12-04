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

const val a = <!EVALUATED: `10`!>getLocalObject(10).getNum()<!>
const val b = <!EVALUATED: `2`!>A(2).localObject.getNum()<!>
