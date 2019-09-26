package dependentOnFile

import kotlin.reflect.KProperty

fun main(args: Array<String>) {
    //Breakpoint!
    args.size
}

class TestClass {
    fun testFun() = 1

    companion object {
        val p = 1
    }
}

fun testFun() = 1
fun Int.testExtFun() = 1

object TestObject {
    val p = 1
}

val testVal = 1
val Int.testExtVal: Int get() = 1
val testDelVal by Delegate()

class Delegate {
    operator fun getValue(a: Any?, b: KProperty<*>) = 1
}

// EXPRESSION: TestClass().testFun()
// RESULT: 1: I

// EXPRESSION: testFun()
// RESULT: 1: I

// EXPRESSION: TestObject.p
// RESULT: 1: I

// EXPRESSION: TestClass.p
// RESULT: 1: I

// EXPRESSION: 1.testExtFun()
// RESULT: 1: I

// EXPRESSION: testVal
// RESULT: 1: I

// EXPRESSION: 1.testExtVal
// RESULT: 1: I

// EXPRESSION: testDelVal
// RESULT: 1: I

