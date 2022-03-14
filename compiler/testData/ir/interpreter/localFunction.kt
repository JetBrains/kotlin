@CompileTimeCalculation
fun returnTheSameValueFun(num: Int) = fun(): Int { return num }

@CompileTimeCalculation
fun multiplyByTwo(num: Int): () -> Int {
    val a = 2
    val b = 5
    return fun(): Int { return num * a }
}

@CompileTimeCalculation
inline fun multiplyBy(noinline multiply: (Int, Int) -> Int): (Int, Int) -> Int {
    return multiply
}

@CompileTimeCalculation
fun checkStackCorrectness(): Int {
    returnTheSameValueFun(10).invoke()
    val num = 5
    return num
}

@CompileTimeCalculation
fun checkLaterInvoke(): String {
    val num = 10
    val function = multiplyByTwo(num)
    val b = 0
    return "previous = " + num + "; multiplied by two = " + function.invoke() + "; b = " + b
}

@CompileTimeCalculation
fun localInline(): String {
    val a = 10
    val b = 20
    val function = multiplyBy() { a, b -> (a + b) * 2 }.invoke(1, 3)
    return "result = " + function + "; (a, b) = ($a, $b)"
}

@CompileTimeCalculation
fun getNumAfterLocalInvoke(): Int {
    var a = 1
    @CompileTimeCalculation
    fun local() {
        a++
    }
    local()
    return a
}

const val a = <!EVALUATED: `2`!>returnTheSameValueFun(2).invoke()<!>
const val b = <!EVALUATED: `2`!>multiplyByTwo(1).invoke()<!>
const val c = <!EVALUATED: `5`!>checkStackCorrectness()<!>
const val d = <!EVALUATED: `previous = 10; multiplied by two = 20; b = 0`!>checkLaterInvoke()<!>
const val e = <!EVALUATED: `result = 8; (a, b) = (10, 20)`!>localInline()<!>
const val f = <!EVALUATED: `2`!>getNumAfterLocalInvoke()<!>
