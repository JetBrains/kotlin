// !LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: NATIVE
// TARGET_BACKEND: WASM
// TARGET_BACKEND: JVM_IR

// IGNORE_BACKEND_K1: ANY

// IllegalArgumentException: arg wrongly != this@Test5: arg=null, this@Test5=[object Object]
// IGNORE_BACKEND_K2: JS_IR, JS_IR_ES6
// Wrong box result 'arg1 wrongly != this@Test5: arg1=Inner@1346131020, this@Test5=Test5@314569418'; Expected "OK"
// IGNORE_BACKEND_K2: WASM

// MODULE: common
// FILE: common.kt

// Default parameter in inline function.
expect inline fun inlineFunction(a: String, b: Int = 0, c: () -> Double? = { null }): String
expect fun test1(x: Int = 42): Int
expect fun test2(x: Int, y: Int = x): Int
expect fun test3(x: Int = 42, y: Int = x + 1): Int
expect fun Int.test6(arg: Int = this): String

expect class Test4 {
    fun test(arg: Any = this): String
}

expect class Test5 {
    inner class Inner {
        constructor(arg: Any = this@Test5)

        fun test(arg1: Any = this@Test5, arg2: Any = this@Inner): String
    }
}

// MODULE: actual()()(common)
// FILE: actual.kt

actual inline fun inlineFunction(a: String, b: Int, c: () -> Double?): String = a + "," + b + "," + c()
actual fun test1(x: Int) = x
actual fun test2(x: Int, y: Int) = x + y
actual fun test3(x: Int, y: Int) = x - y
actual fun Int.test6(arg: Int): String {
    if (arg != this)
        return "arg wrongly != this: arg=$arg, this=$this"
    return "OK"
}

actual class Test4 {
    actual fun test(arg: Any): String {
        if (arg != this)
            return "arg wrongly != this: arg=$arg, this=$this"
        return "OK"
    }
}

actual class Test5 {
    actual inner class Inner {
        actual constructor(arg: Any) {
            if (arg != this@Test5)
                throw IllegalArgumentException("arg wrongly != this@Test5: arg=$arg, this@Test5=${this@Test5}")
        }

        actual fun test(arg1: Any, arg2: Any): String {
            if (arg1 != this@Test5)
                return "arg1 wrongly != this@Test5: arg1=$arg1, this@Test5=${this@Test5}"
            if (arg2 != this@Inner)
                return "arg2 wrongly != this@Inner: arg2=$arg2, this@Inner=${this@Inner}"
            return "OK"
        }
    }
}

fun box(): String {
    val test1 = test1()
    if (test1 != 42)
        return "test1 is wrongly $test1"

    val test2 = test2(17)
    if (test2 != 34)
        return "test2 is wrongly $test2"

    val test3 = test3()
    if (test3 != -1)
        return "test3 is wrongly $test3"

    val test4 = Test4().test()
    if (test4 != "OK")
        return test4

    val test5 = Test5().Inner().test()
    if (test5 != "OK")
        return test5

    val test6 = 42.test6()
    if (test6 != "OK")
        return test6

    val inlineFunctionResult = inlineFunction("OK")
    if (inlineFunctionResult != "OK,0,null")
        return "inlineFunctionResult is wrongly $inlineFunctionResult"

    return "OK"
}
