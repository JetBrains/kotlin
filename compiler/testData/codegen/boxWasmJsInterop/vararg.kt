// Partial copy of js/js.translator/testData/box/native/vararg.kt
// With some additions for concrete number types, strings and function references

package foo

import kotlin.test.assertEquals

external fun paramCount(vararg a: Int): Int = definedExternally

@JsName("paramCount")
external fun anotherParamCount(vararg a: Int): Int = definedExternally

// test spread operator
fun count(vararg a: Int) = paramCount(*a)

// test spread operator
fun anotherCount(vararg a: Int) = anotherParamCount(*a)

external fun test3(bar: Bar, dummy: Int, vararg args: Int): Boolean = definedExternally

external class Bar(size: Int, order: Int = definedExternally) {
    val size: Int
    fun test(order: Int, dummy: Int, vararg args: Int): Boolean = definedExternally
    companion object {
        fun startNewTest(): Boolean = definedExternally
        var hasOrderProblem: Boolean = definedExternally
    }
}

external object obj {
    fun test(size: Int, vararg args: Int): Boolean = definedExternally
}

fun spreadInMethodCall(size: Int, vararg args: Int) = Bar(size).test(0, 1, *args)

fun spreadInObjectMethodCall(size: Int, vararg args: Int) = obj.test(size, *args)

fun spreadInPackageMethodCall(size: Int, vararg args: Int) = test3(Bar(size), 1, *args)

fun testSpreadOperatorWithSafeCall(a: Bar?, expected: Boolean?, vararg args: Int): Boolean {
    return a?.test(0, 1, *args) == expected
}

fun testSpreadOperatorWithSureCall(a: Bar?, vararg args: Int): Boolean {
    return a!!.test(0, 1, *args)
}

fun testCallOrder(vararg args: Int) =
        Bar.startNewTest() &&
        Bar(args.size, 0).test(1, 1, *args) && Bar(args.size, 2).test(3, 1, *args) &&
        !Bar.hasOrderProblem

external fun sumOfParameters(x: Int, y: Int, vararg a: Int): Int = definedExternally

external fun sumFunValuesOnParameters(x: Int, y: Int, vararg a: Int, f: (Int) -> Int): Int = definedExternally

fun runCallable(fn: (Int, Int, Int, Int, Int) -> Int, a1: Int, a2: Int, a3: Int, a4: Int, a5: Int): Int = fn(a1, a2, a3, a4, a5)

@JsName("join")
external fun joinBoolean(vararg x: Boolean): String

@JsName("join")
external fun joinByte(vararg x: Byte): String

@JsName("join")
external fun joinChar(vararg x: Char): String

@JsName("join")
external fun joinShort(vararg x: Short): String

@JsName("join")
external fun joinInt(vararg x: Int): String

@JsName("join")
external fun joinLong(vararg x: Long): String

@JsName("join")
external fun joinFloat(vararg x: Float): String

@JsName("join")
external fun joinDouble(vararg x: Double): String

@JsName("join")
external fun joinString(vararg x: String): String

@JsName("mapJoin")
external fun mapJoin(f: ((String) -> String) -> String, vararg x: (String) -> String): String

@JsName("join")
external fun joinWithDefault(a: Int = definedExternally, b: Int = definedExternally, vararg c: Int, d: Int = definedExternally): String

fun box(): String {
    if (paramCount() != 0)
        return "failed when call native function without args"

    if (paramCount(1) != 1) return "failed when call native function with single vararg"

    if (paramCount(1, 2, 3) != 3)
        return "failed when call native function with some args"

    if (anotherParamCount(1, 2, 3) != 3)
        return "failed when call native function with some args witch declareted with custom name"

    if (count() != 0)
        return "failed when call native function without args using spread operator"

    if (count(1, 1, 1, 1) != 4)
        return "failed when call native function with some args using spread operator"

    if (anotherCount(1, 2, 3) != 3)
        return "failed when call native function with some args using spread operator witch declareted with custom name"

    if (!Bar(5).test(0, 1, 1, 2, 3, 4, 5))
        return "failed when call method with some args"

    if (!spreadInMethodCall(2, 1, 2))
        return "failed when call method using spread operator"

    if (!Bar(1).test(0, 1, 1))
        return "failed when call method with single arg"

    if (!spreadInMethodCall(2, 1, 2))
        return "failed when call method using spread operator"

    if (!(obj.test(5, 1, 2, 3, 4, 5)))
        return "failed when call method of object"

    if (!(spreadInObjectMethodCall(2, 1, 2)))
        return "failed when call method of object using spread operator"

    if (!spreadInPackageMethodCall(2, 1, 2))
        return "failed when call package method using spread operator"

    if (!(testSpreadOperatorWithSafeCall(null, null)))
        return "failed when test spread operator with SafeCall (?.) using null receiver"

    if (!(testSpreadOperatorWithSafeCall(Bar(3), true, 1, 2, 3)))
        return "failed when test spread operator with SafeCall (?.)"

    if (!(testSpreadOperatorWithSureCall(Bar(3), 1, 2, 3)))
        return "failed when test spread operator with SureCall (!!)"

    if (!(testCallOrder()))
        return "failed when test calling order when using spread operator without args"

    if (!(testCallOrder(1, 2, 3, 4)))
        return "failed when test calling order when using spread operator with some args"

    val baz: Bar? = Bar(1)
    if (!(baz!!)?.test(0, 1, 1)!!)
        return "failed when combined SureCall and SafeCall, maybe we lost cached expression"

    assertEquals(45, sumOfParameters(1, 2, 3, 4, 5, 6, 7, 8, 9))
    assertEquals(45, sumOfParameters(1, 2, *intArrayOf(3, 4, 5, 6, 7, 8, 9)))
    assertEquals(45, sumOfParameters(1, 2, a = *intArrayOf(3, 4, 5, 6, 7, 8, 9)))
    assertEquals(45, sumOfParameters(1, 2, a = intArrayOf(3, 4, 5, 6, 7, 8, 9)))
    assertEquals(45, sumOfParameters(1, 2, 3, 4, *intArrayOf(5, 6, 7, 8, 9)))
    assertEquals(90, sumFunValuesOnParameters(1, 2, 3, 4, 5, 6, 7, 8, 9) { 2*it })
    assertEquals(90, sumFunValuesOnParameters(1, 2, *intArrayOf(3, 4, 5, 6, 7, 8, 9)) { 2*it })
    assertEquals(90, sumFunValuesOnParameters(1, 2, a = *intArrayOf(3, 4, 5, 6, 7, 8, 9)) { 2*it })
    assertEquals(90, sumFunValuesOnParameters(1, 2, a = intArrayOf(3, 4, 5, 6, 7, 8, 9)) { 2*it })
    assertEquals(90, sumFunValuesOnParameters(1, 2, 3, 4, *intArrayOf(5, 6, 7, 8, 9)) { 2*it })
    assertEquals(90, sumFunValuesOnParameters(1, 2, *intArrayOf(3, 4, 5, 6, 7), 8, 9) { 2*it })
    assertEquals(90, sumFunValuesOnParameters(1, 2, *intArrayOf(3, 4, 5), *intArrayOf(6, 7, 8, 9)) { 2*it })
    assertEquals(90, sumFunValuesOnParameters(1, 2, *intArrayOf(3, 4), 5, 6, *intArrayOf(7, 8, 9)) { 2*it })

    assertEquals(5, runCallable(::paramCount, 1, 2, 3, 4, 5))
    assertEquals(5, runCallable(::anotherParamCount, 1, 2, 3, 4, 5))
    assertEquals(5, runCallable(::anotherCount, 1, 2, 3, 4, 5))
    assertEquals(11111, runCallable(::sumOfParameters, 1, 10, 100, 1000, 10000))

    if (joinBoolean(true, false, true) != "true-false-true") return "Fail1"
    if (joinByte(10.toByte(), 20.toByte(), 30.toByte()) != "10-20-30") return "Fail2"
    if (joinChar(10.toChar(), 20.toChar(), 30.toChar()) != "10-20-30") return "Fail3"
    if (joinShort(10.toShort(), 20.toShort(), 30.toShort()) != "10-20-30") return "Fail4"
    if (joinInt(10.toInt(), 20.toInt(), 30.toInt()) != "10-20-30") return "Fail5"
    if (joinLong(10.toLong(), 20.toLong(), 30.toLong()) != "10-20-30") return "Fail6"
    if (joinFloat(10.toFloat(), 20.toFloat(), 30.toFloat()) != "10-20-30") return "Fail7"
    if (joinDouble(10.toDouble(), 20.toDouble(), 30.toDouble()) != "10-20-30") return "Fail8"

    if (joinString("a", "b", "c") != "a-b-c") return "Fail9"

    if (
        mapJoin(
            f = { f -> f("a")},
            { it + "10" },
            { it + "20" },
            { it + "30" }
        ) != "a10-a20-a30"
    ) { return "Fail10" }

    if (joinWithDefault(10, 20, 30, 40, d = 50) != "10-20-30-40-50") return "Fail11"

    return "OK"
}
