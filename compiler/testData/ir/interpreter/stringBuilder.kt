@CompileTimeCalculation
fun appendVararg(vararg strings: String): String {
    val sb = StringBuilder()
    for (string in strings) {
        sb.append(string)
    }
    return sb.toString()
}


const val simpleAppend = <!EVALUATED: `str`!>StringBuilder().append("str").toString()<!>
const val withCapacity = <!EVALUATED: `example`!>StringBuilder(7).append("example").toString()<!>
const val withContent = <!EVALUATED: `first second`!>StringBuilder("first").append(" ").append("second").toString()<!>
const val appendInFun = <!EVALUATED: `1 2 3`!>appendVararg("1", " ", "2", " ", "3")<!>

const val length1 = <!EVALUATED: `1`!>StringBuilder(3).append("1").length<!>
const val length2 = <!EVALUATED: `9`!>StringBuilder().append("123456789").length<!>
const val get0 = <!EVALUATED: `1`!>StringBuilder().append("1234556789").get(0)<!>
const val get1 = <!EVALUATED: `2`!>StringBuilder().append("1234556789").get(1)<!>
const val subSequence1 = <!EVALUATED: `12`!>StringBuilder().append("123456789").subSequence(0, 2) as String<!>
const val subSequence2 = <!EVALUATED: `345678`!>StringBuilder().append("123456789").subSequence(2, 8) as String<!>

const val appendPart = <!EVALUATED: `23`!>StringBuilder().append("123456789", 1, 3).toString()<!>
const val appendNull = <!EVALUATED: `null`!>StringBuilder().append(null as Any?).toString()<!>
