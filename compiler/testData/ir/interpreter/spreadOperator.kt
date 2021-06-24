// IGNORE_BACKEND: JVM_IR
@CompileTimeCalculation
fun varargSum(quantityToSum: Int, vararg num: Int): Int {
    var sum = 0
    for (i in 0..(quantityToSum - 1)) {
        if (i >= num.size) return sum
        sum += num[i]
    }
    return sum
}

@CompileTimeCalculation
fun sumWithVarargAtFirst(vararg num: Int, quantityToSum: Int = num.size) = varargSum(quantityToSum, *num)

@CompileTimeCalculation
fun concatenation(vararg str: String): String {
    var result = ""
    for (s in str) result += s
    return result
}

@CompileTimeCalculation
fun customArrayConcatenation(vararg array: Any?): String {
    var result = ""
    for (elem in array) result += elem.toString() + " "
    return result
}

const val a1 = <!EVALUATED: `6`!>varargSum(3, 1, 2, 3)<!>
const val a2 = <!EVALUATED: `3`!>varargSum(num = *intArrayOf(1, 2, 3), quantityToSum = 2)<!>
const val a3 = <!EVALUATED: `21`!>varargSum(6, *intArrayOf(1, 2), 3, *intArrayOf(4, 5, 6))<!>

const val b1 = <!EVALUATED: `6`!>sumWithVarargAtFirst(1, 2, 3)<!>
const val b2 = <!EVALUATED: `6`!>sumWithVarargAtFirst(quantityToSum = 3, num = *intArrayOf(1, 2, 3))<!>

const val c1 = <!EVALUATED: `1 2 3`!>concatenation(*arrayOf("1", " "), "2", *arrayOf(" ", "3"))<!>
const val c2 = <!EVALUATED: `1 2.0 3 -1 -2.0 `!>customArrayConcatenation(*arrayOf(1, 2.0), "3", *arrayOf(-1, -2.0))<!>
