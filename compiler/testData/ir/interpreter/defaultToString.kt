@CompileTimeCalculation
open class A

class B @CompileTimeCalculation constructor() {
    @CompileTimeCalculation
    override fun toString(): String {
        return super.toString()
    }
}

class C

class D : A()

@CompileTimeCalculation
fun checkToStringCorrectness(value: Any, startStr: String): Boolean {
    val string = value.toString()
    return string.subSequence(0, startStr.length) == startStr && string.get(startStr.length) == '@' && string.length <= startStr.length + 9
}

@CompileTimeCalculation
fun getTheSameValue(a: Any): Any = a

@CompileTimeCalculation
fun theSameObjectToString(value: Any): Boolean {
    return value.toString() == getTheSameValue(value).toString()
}

const val aString = <!EVALUATED: `true`!>checkToStringCorrectness(A(), "A")<!>
const val bString = <!EVALUATED: `true`!>checkToStringCorrectness(B(), "B")<!>
val cString = C().toString() // must not be calculated
val dString = D().toString() // must not be calculated

const val arrayString = <!EVALUATED: `true`!>checkToStringCorrectness(arrayOf(A(), B()).toString(), "[Ljava.lang.Object;")<!>
const val intArrayString = <!EVALUATED: `true`!>checkToStringCorrectness(intArrayOf(1, 2, 3).toString(), "[I")<!>

const val checkA = <!EVALUATED: `true`!>theSameObjectToString(A())<!>
const val checkStringBuilder1 = <!EVALUATED: `true`!>theSameObjectToString(StringBuilder())<!>
const val checkStringBuilder2 = <!EVALUATED: `true`!>theSameObjectToString(StringBuilder("Some Builder"))<!>
