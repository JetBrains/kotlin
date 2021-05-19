@CompileTimeCalculation
open class A {
    open fun String.getSize() = this.length

    fun returnSizeOf(str: String) = str.getSize()
}

@CompileTimeCalculation
class B : A() {
    override fun String.getSize() = -1
}

const val a = A().<!EVALUATED: `4`!>returnSizeOf("1234")<!>
const val b = B().<!EVALUATED: `-1`!>returnSizeOf("1234")<!>
