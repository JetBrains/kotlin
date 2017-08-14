// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

import kotlin.internal.*

fun <T> myRun(@CalledInPlace(InvocationCount.EXACTLY_ONCE) block: () -> T) = block()

fun initialization() {
    val x: Int
    myRun {
        x = 42
    }
    x.inc()
}

fun shadowing() {
    val x = 42
    myRun {
        val <!NAME_SHADOWING!>x<!> = 43
        x.inc()
    }
    x.inc()
}

fun nestedDefiniteAssignment() {
    val x: Int
    myRun {
        val y = "Hello"
        myRun {
            x = 42
        }
        y.length
    }
    x.inc()
}

fun deeplyNestedDefiniteAssignment() {
    val x: Int
    myRun {
        val y: String
        myRun {
            val z: String
            myRun {
                z = "Hello"
                y = "World"
                x = 42
            }
            z.length
        }
        y.length
    }
    x.inc()
}

fun branchingFlow(a: Any?) {
    val x: Int

    if (a is String) {
        myRun { x = 42 }
    } else {
        myRun { x = 43 }
    }

    x.inc()
}

fun returningValue() {
    val x: Int
    val hello = myRun { x = 42; "hello" }
    x.inc()
    hello.length
}

class DefiniteInitializationInInitSection {
    val x: Int

    init {
        myRun { x = 42 }
    }
}