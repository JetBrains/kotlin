// ISSUE: KT-73921
// Please keep these tests in folder `compiler/testData/codegen/box/diagnostics/functions/tailRecursion`,
//   then investigations of failed tests due to big stack depth would be simpler:
//   - should NO_TAIL_CALLS_FOUND also appear, then it's an issue in either test source or frontend or FIR checker.
//   - otherwise, it would be a problem in FIR2IR, or IR lowerings including TailrecLowering
// TARGET_BACKEND: JVM_IR
//   Only K/JVM has standard stable way to determine the size of current VM call stack

private val getStackTraceMethod = Thread::class.java.getMethod("getStackTrace")
private var vmStackDepth = 0

private fun vmStackDepthSnapShot() {
    val stackTrace = getStackTraceMethod.invoke(Thread.currentThread()) as Array<*>
    vmStackDepth = stackTrace.size
}

fun checkShallowStackDepth(lambda: () -> Unit) {
    vmStackDepthSnapShot()
    val vmDepthBaseline = vmStackDepth
    lambda()
    if(vmStackDepth - vmDepthBaseline > 4) // In CodegenTestsOnAndroidRunner, stack depth reaches 4
        error("Fail: TailrecLowering did not happen, VM stack depth=${vmStackDepth - vmDepthBaseline}")
    if(vmStackDepth == vmDepthBaseline)
        error("Test source issue: vmStackDepth was not changed after tailrec call. Insert `vmStackDepthSnapShot()` as the first operator in tailrec fun")
}

tailrec fun addNodeTest1(node: IntArray, level: Int) {
    vmStackDepthSnapShot()
    if (level <= 0) {
        node[level]
        return
    }
    addNodeTest1(node, level - 1)
}

// From previous function, replace `return` with `else`, semantics doesn't change, so the function still must be a tailrec
tailrec fun addNodeTest2(node: IntArray, level: Int) {
    vmStackDepthSnapShot()
    if (level <= 0) {
        node[level]
    } else {
        addNodeTest2(node, level - 1)
    }
}

// Invert `if`, still must be detected as tailrec
tailrec fun addNodeTest3(node: IntArray, level: Int) {
    vmStackDepthSnapShot()
    if (level > 0) {
        addNodeTest3(node, level - 1)
    } else {
        node[level]
    }
}

// Add return, still must be detected as tailrec
tailrec fun addNodeTest4(node: IntArray, level: Int) {
    vmStackDepthSnapShot()
    if (level > 0) {
        addNodeTest4(node, level - 1)
        return // required to be last call for tailrec fun
    } else {
        node[level]
    }
}

// From tailCallInParentheses.kt
tailrec fun addNodeTest5(node: IntArray, level: Int) {
    vmStackDepthSnapShot()
    if (level <= 0) return
    return (addNodeTest5(node, level - 1))
}

// From tailCallInBlockInParentheses.kt
tailrec fun addNodeTest6(node: IntArray, level: Int) {
    vmStackDepthSnapShot()
    return if (level > 0) {
        (addNodeTest6(node, level - 1))
    } else {
        node[level]
        @Suppress("UNUSED_EXPRESSION")
        Unit
    }
}

// From returnInParentheses.kt
tailrec fun addNodeTest7(node: IntArray, level: Int) {
    vmStackDepthSnapShot()
    if (level <= 0) {
        node[level]
        return
    }
    (return addNodeTest7(node, level - 1))
}

// From extensionTailCall.kt
tailrec fun Int.addNodeTest8(level: Int) {
    vmStackDepthSnapShot()
    if (level == 0) return
    return 1.addNodeTest8(level - 1)
}

// From sum.kt
tailrec fun addNodeTest9(level: Long, sum: Long): Long {
    vmStackDepthSnapShot()
    if (level == 0L) return sum
    return addNodeTest9(level - 1, sum + level)
}

// From whenWithIs.kt
tailrec fun addNodeTest10(counter: Int, payload: Any): Int {
    vmStackDepthSnapShot()
    return if (counter == 0) {
        0
    } else if (counter == 5) {
        addNodeTest10(counter - 1, 999)
    } else {
        when (payload) {
            is String -> addNodeTest10(counter - 1, "is String")
            is Number -> addNodeTest10(counter, "is Number")
            else -> error("Unexpected payload")
        }
    }
}

// From whenWithLambda.kt
tailrec fun addNodeTest11(counter: Int): Int {
    vmStackDepthSnapShot()
    return when (counter) {
        0 -> addNodeTest11(1)
        1 -> counter
        else -> run { null } ?: addNodeTest11(counter - 1)
    }
}

// Similar to whenWithIs.kt, but with nullable payload to force extra type adaptation in branch results
tailrec fun addNodeTest12(counter: Int, payload: Any?): Any {
    vmStackDepthSnapShot()
    return if (counter == 0) {
        payload ?: "done"
    } else {
        when (payload) {
            is String? -> addNodeTest12(counter - 1, payload)
            is Number? -> addNodeTest12(counter - 1, payload)
            else -> addNodeTest12(counter - 1, "fallback")
        }
    }
}

open class Base
class Derived : Base()
tailrec fun test13(n: Int): Base {
    vmStackDepthSnapShot()
    return if (n == 0) // compiler may insert IMPLICIT_CAST somewhere to cast Derived to Base
        Derived()
    else
        test13(n - 1)  // recursive call returns Base
}

fun box(): String {
    val intArray = IntArray(42) { 42 }
    checkShallowStackDepth { addNodeTest1(intArray, 10) }
    checkShallowStackDepth { addNodeTest2(intArray, 10) }
    checkShallowStackDepth { addNodeTest3(intArray, 10) }
    checkShallowStackDepth { addNodeTest4(intArray, 10) }
    checkShallowStackDepth { addNodeTest5(intArray, 10) }
    checkShallowStackDepth { addNodeTest6(intArray, 10) }
    checkShallowStackDepth { addNodeTest7(intArray, 10) }
    checkShallowStackDepth { 1.addNodeTest8(20) }
    checkShallowStackDepth {
        val sum = addNodeTest9(20L, 0L)
        if (sum != 20L * 21L / 2L) {
            error("Unexpected sum result: $sum")
        }
    }
    checkShallowStackDepth {
        val result = addNodeTest10(20, "test")
        if (result != 0) {
            error("Unexpected when-is result: $result")
        }
    }
    checkShallowStackDepth {
        val result = addNodeTest11(20)
        if (result != 1) {
            error("Unexpected when-lambda result: $result")
        }
    }
    checkShallowStackDepth {
        val result = addNodeTest12(20, "test")
        if (result != "test") {
            error("Unexpected nullable-when result: $result")
        }
    }
    checkShallowStackDepth { test13(10) }
    return "OK"
}
