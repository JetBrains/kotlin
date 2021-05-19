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

const val simple = 1.<!EVALUATED: `1`!>get()<!>
const val right = A(3).<!EVALUATED: `true`!>check("123")<!>
const val wrong = A(2).<!EVALUATED: `false`!>check("123")<!>
