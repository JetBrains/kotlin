@CompileTimeCalculation
fun Int.get(): Int {
    return this
}

@CompileTimeCalculation
class A(val length: Int) {
    fun String.hasRightLength(): Boolean {
        return this@hasRightLength.length == this@A.length
    }

    fun check(string: String): Boolean {
        return string.hasRightLength()
    }
}

const val simple = <!EVALUATED: `1`!>1.get()<!>
const val right = <!EVALUATED: `true`!>A(3).check("123")<!>
const val wrong = <!EVALUATED: `false`!>A(2).check("123")<!>
