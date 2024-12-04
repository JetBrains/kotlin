@CompileTimeCalculation
open class A {
    open fun String.getSize() = this.length

    fun returnSizeOf(str: String) = str.getSize()
}

@CompileTimeCalculation
class B : A() {
    override fun String.getSize() = -1
}

const val a = <!EVALUATED: `4`!>A().returnSizeOf("1234")<!>
const val b = <!EVALUATED: `-1`!>B().returnSizeOf("1234")<!>
