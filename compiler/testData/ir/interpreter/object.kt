@CompileTimeCalculation
class A {
    companion object {
        const val static = <!EVALUATED: `-10`!>{ -10 }()<!> // lambda is needed to avoid computions by old frontend

        fun getStaticNumber(): Int {
            return Int.MAX_VALUE
        }
    }
}

@CompileTimeCalculation
object ObjectWithConst {
    const val a = 100
    const val b = <!EVALUATED: `Value in a: 100`!>concat("Value in a: ", a)<!>

    val nonConst = { "Not const field in compile time object" }()

    fun concat(first: String, second: Any) = "$first$second"
}

const val numStatic = <!EVALUATED: `-10`!>A.static<!>
const val numStaticFromFun = <!EVALUATED: `2147483647`!>A.getStaticNumber()<!>
const val valFromObject = <!EVALUATED: `Value in a: 100`!>ObjectWithConst.b<!>
const val valFnonConstFromObject = <!EVALUATED: `Not const field in compile time object`!>ObjectWithConst.nonConst<!>
