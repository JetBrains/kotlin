// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun barRegular(f: (Int) -> Unit) {}
fun barRegularEmpty(f: () -> Unit) {}

private fun testRepeated() {
    var repeat = true
    var attempts = 0
    while (repeat) {
        barRegular { index ->
            try {
                println(attempts)
                <!CV_DIAGNOSTIC!>repeat<!> = false
            } catch (e: Throwable) {
                println(e)
            }
        }
    }
}

fun testNestedFunction() {
    var r = 2

    fun localHelper() {
        barRegularEmpty {
            println(r)
        }
    }

    localHelper()
    r = 4
    localHelper()
}

fun testNestedAnonymousFunction() {
    var outer = "a"
    fun localHelper() {
        var l = 3
        barRegularEmpty {
            println(<!CV_DIAGNOSTIC!>l<!>)
            println(outer) // problem
        }
        l = 2
    }
    outer = "b"
}

fun testNestedConstructor() {
    var l = 2
    class Local {
        constructor(i: Int) {
            val result = i + l
            println("Captured l: $l")
        }
    }

    Local(10) // Captured l: 2
    l = 5
    Local(10) // Captured l: 5
}

fun testNestedConstructorWithAnonymous() {
    var l = 2
    var r = 2

    class Local {
        constructor(i: Int) {
            var result = i + l
            barRegularEmpty {
                println(<!CV_DIAGNOSTIC!>l<!>)
                println(<!CV_DIAGNOSTIC!>result<!>)
                println(<!CV_DIAGNOSTIC!>r<!>)
            }
            result = 3
            l = 3
        }
    }

    Local(10)

    r = 2
    l = 5
    Local(10)
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration, tryExpression, whileLoop */
