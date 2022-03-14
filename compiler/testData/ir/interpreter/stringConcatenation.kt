import kotlin.*

@CompileTimeCalculation
open class A {
    override fun toString(): String {
        return "toString call from A class"
    }
}

@CompileTimeCalculation
class B : A()

@CompileTimeCalculation
class C

class D @CompileTimeCalculation constructor() {
    @CompileTimeCalculation
    override fun toString(): String {
        return super.toString()
    }
}

@CompileTimeCalculation
fun getDoubledValue(a: Int): Int {
    return 2 * a
}

@CompileTimeCalculation
fun checkToStringCorrectness(value: Any, startSymbol: Char): Boolean {
    val string = value.toString()
    return string.get(0) == startSymbol && string.get(1) == '@' && string.length == 10
}

@CompileTimeCalculation fun echo(value: String) = value
@CompileTimeCalculation fun concat(first: String, second: Any) = "$first$second"

const val constStr = <!EVALUATED: `Success`!>echo("Success")<!>
const val concat1 = <!EVALUATED: `String concatenation example: Success`!>concat("String concatenation example: ", constStr)<!>
const val concat2 = <!EVALUATED: `String concatenation example with primitive: 1`!>concat("String concatenation example with primitive: ", 1)<!>
const val concat3 = <!EVALUATED: `String concatenation example with primitive and explicit toString call: 1`!>concat("String concatenation example with primitive and explicit toString call: ", 1.toString())<!>
const val concat4 = <!EVALUATED: `String concatenation example with function that return primitive: 20`!>"String concatenation example with function that return primitive: ${getDoubledValue(10)}"<!>
const val concat5 = <!EVALUATED: `String concatenation example with A class: toString call from A class`!>"String concatenation example with A class: ${A()}"<!>
const val concat6 = <!EVALUATED: `String concatenation example with B class, where toString is FAKE_OVERRIDDEN: toString call from A class`!>"String concatenation example with B class, where toString is FAKE_OVERRIDDEN: ${B()}"<!>
const val concat7 = <!EVALUATED: `String concatenation example with B class and explicit toString call: toString call from A class`!>"String concatenation example with B class and explicit toString call: ${B().toString()}"<!>
const val concat8 = <!EVALUATED: `String concatenation example with C class, where toString isn't present; is it correct: true`!>"String concatenation example with C class, where toString isn't present; is it correct: ${checkToStringCorrectness(C(), 'C')}"<!>
const val concat9 = <!EVALUATED: `String concatenation example with D class, where toString is taken from Any; is it correct: true`!>"String concatenation example with D class, where toString is taken from Any; is it correct: ${checkToStringCorrectness(D(), 'D')}"<!>

const val concat10 = <!EVALUATED: `String plus example with A class: toString call from A class`!>"String plus example with A class: " + A()<!>
const val concat11 = <!EVALUATED: `String plus example with B class, where toString is FAKE_OVERRIDDEN: toString call from A class`!>"String plus example with B class, where toString is FAKE_OVERRIDDEN: " + B()<!>

const val concatLambda1 = <!EVALUATED: `() -> kotlin.String`!>"" + fun(): String = ""<!>
const val concatLambda2 = <!EVALUATED: `() -> kotlin.String`!>"" + (fun(): String = "").toString()<!>
const val concatLambda3 = <!EVALUATED: `() -> kotlin.String`!>"" + fun(): String = "Some string"<!>
const val concatLambda4 = <!EVALUATED: `(kotlin.Int) -> kotlin.String`!>"" + fun(i: Int): String = ""<!>
const val concatLambda5 = <!EVALUATED: `(kotlin.Int?) -> kotlin.String?`!>"" + fun(i: Int?): String? = ""<!>
const val concatLambda6 = <!EVALUATED: `(kotlin.Int) -> kotlin.String`!>"" + { i: Int -> "" }<!>
const val concatLambda7 = <!EVALUATED: `() -> kotlin.Unit`!>"" + {  }<!>
const val concatLambda8 = <!EVALUATED: `(kotlin.Int, kotlin.Double, kotlin.String) -> kotlin.Unit`!>"" + { i: Int, b: Double, c: String ->  }<!>
const val concatLambda9 = <!EVALUATED: `kotlin.Double.(kotlin.Int) -> kotlin.String`!>"".let {
    val lambdaWith: Double.(Int) -> String = { "A" }
    lambdaWith.toString()
}<!>

// wrap as lambda to prevent calculations on frontend
const val extensionPlus1 = <!EVALUATED: `Null as string = null`!>{ "Null as string = " + null }()<!>
const val extensionPlus2 = <!EVALUATED: `Null as string = null`!>"Null as string = ${null.toString()}"<!>
