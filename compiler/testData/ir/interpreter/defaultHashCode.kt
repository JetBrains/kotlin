@CompileTimeCalculation
open class A

class B @CompileTimeCalculation constructor() {
    @CompileTimeCalculation
    override fun hashCode(): Int {
        return super.hashCode()
    }
}

class C

class D : A()

@CompileTimeCalculation
fun checkHashCodeCorrectness(value: Any): Boolean {
    val hashCode = value.hashCode()
    return hashCode.toHex().length == 8 && hashCode == value.hashCode()
}

@CompileTimeCalculation
fun getTheSameValue(a: Any): Any = a

@CompileTimeCalculation
fun theSameObjectHashCode(value: Any): Boolean {
    return value.hashCode() == getTheSameValue(value).hashCode()
}

@CompileTimeCalculation
fun Int.toHex(): String {
    val sb = StringBuilder()
    val hexDigits = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    )
    var value = this

    var i = 8
    while (i > 0) {
        i -= 1
        val j = value.and(0x0F)
        sb.append(hexDigits[j])
        value = value.shr(4)
    }

    return sb.reverse().toString()
}

const val aHashCode = <!EVALUATED: `true`!>checkHashCodeCorrectness(A())<!>
const val bHashCode = <!EVALUATED: `true`!>checkHashCodeCorrectness(B())<!>
val cHashCode = C().hashCode() // must not be calculated
val dHashCode = D().hashCode() // must not be calculated

const val arrayHashCode = <!EVALUATED: `true`!>checkHashCodeCorrectness(arrayOf(A(), B()))<!>
const val intArrayHashCode = <!EVALUATED: `true`!>checkHashCodeCorrectness(arrayOf(1, 2, 3))<!>

const val checkA = <!EVALUATED: `true`!>theSameObjectHashCode(A())<!>
const val checkStringBuilder1 = <!EVALUATED: `true`!>theSameObjectHashCode(StringBuilder())<!>
const val checkStringBuilder2 = <!EVALUATED: `true`!>theSameObjectHashCode(StringBuilder("Some Builder"))<!>
