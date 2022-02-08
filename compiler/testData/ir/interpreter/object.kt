@CompileTimeCalculation
class A {
    const val a = <!EVALUATED: `10`!>{ 10 }()<!> // lambda is needed to avoid computions by old frontend

    companion object {
        const val static = <!EVALUATED: `-10`!>{ -10 }()<!>

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

const val num = A().<!EVALUATED: `10`!>a<!>
const val numStatic = A.<!EVALUATED: `-10`!>static<!>
const val numStaticFromFun = A.<!EVALUATED: `2147483647`!>getStaticNumber()<!>
const val valFromObject = ObjectWithConst.<!EVALUATED: `Value in a: 100`!>b<!>
const val valFnonConstFromObject = ObjectWithConst.<!EVALUATED: `Not const field in compile time object`!>nonConst<!>
