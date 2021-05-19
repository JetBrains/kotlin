@CompileTimeCalculation
fun appendVararg(vararg strings: String): String {
    val sb = StringBuilder()
    for (string in strings) {
        sb.append(string)
    }
    return sb.toString()
}


const val simpleAppend = StringBuilder().append("str").<!EVALUATED: `str`!>toString()<!>
const val withCapacity = StringBuilder(7).append("example").<!EVALUATED: `example`!>toString()<!>
const val withContent = StringBuilder("first").append(" ").append("second").<!EVALUATED: `first second`!>toString()<!>
const val appendInFun = <!EVALUATED: `1 2 3`!>appendVararg("1", " ", "2", " ", "3")<!>

const val length1 = StringBuilder(3).append("1").<!EVALUATED: `1`!>length<!>
const val length2 = StringBuilder().append("123456789").<!EVALUATED: `9`!>length<!>
const val get0 = StringBuilder().append("1234556789").<!EVALUATED: `1`!>get(0)<!>
const val get1 = StringBuilder().append("1234556789").<!EVALUATED: `2`!>get(1)<!>
const val subSequence1 = <!EVALUATED: `12`!>StringBuilder().append("123456789").subSequence(0, 2) as String<!>
const val subSequence2 = <!EVALUATED: `345678`!>StringBuilder().append("123456789").subSequence(2, 8) as String<!>

const val appendPart = StringBuilder().append("123456789", 1, 3).<!EVALUATED: `23`!>toString()<!>
const val appendNull = StringBuilder().append(null as Any?).<!EVALUATED: `null`!>toString()<!>
