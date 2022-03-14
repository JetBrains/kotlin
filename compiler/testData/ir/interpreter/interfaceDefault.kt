@CompileTimeCalculation
interface A {
    fun getInt(): Int

    fun getStr(): String = "Number is ${getInt()}"
}

@CompileTimeCalculation
class B(val b: Int) : A {
    override fun getInt(): Int = b

    fun getStrFromB() = "B " + super.getStr()
}

const val str1 = <!EVALUATED: `Number is 5`!>B(5).getStr()<!>
const val str2 = <!EVALUATED: `B Number is 5`!>B(5).getStrFromB()<!>

@CompileTimeCalculation
interface C {
    val num: Int
    fun getInt() = num
}

@CompileTimeCalculation
class D(override val num: Int) : C {
    fun getStr() = "D num = " + super.getInt()
}

const val num1 = <!EVALUATED: `10`!>D(10).getInt()<!>
const val num2 = <!EVALUATED: `D num = 10`!>D(10).getStr()<!>
