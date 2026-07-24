// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// FULL_JDK
// WITH_EXTRA_CHECKERS
// LANGUAGE: +ReportEscapingCapturedVariable

fun baz(s: String) {}

private fun testReassignmentAcrossMultipleLambdas() {
    var r = 1

    Thread { r = 3 }
    Thread { r = 4 }
    println(r)
}

fun testReturnThread(): Thread {
    var isScheduled = false
    Thread { isScheduled = true }
    return Thread {
        if (!<!ESCAPING_CAPTURED_VARIABLE!>isScheduled<!>) {
            isScheduled = true
            baz("scheduled")
            isScheduled = false
        }
    }
}

class MutableObject(var mutableField: String = "initial")
fun testObjectReassignmentAcrossLambdas() {
    var mutObj = MutableObject()

    Thread {
        mutObj = MutableObject("process")
        println(mutObj.mutableField)
    }

    Thread {
        println(<!ESCAPING_CAPTURED_VARIABLE!>mutObj<!>.toString())
    }
}

fun testStringReassignment() {
    var x = "bla"

    Thread { x = "3" }
    Thread { println(<!ESCAPING_CAPTURED_VARIABLE!>x<!>) }
}

fun testSmartCastReassignedInAnotherLambda() {
    var flag = true
    var name = "World"
    var obj: Any = "text"
    var nullableStr: String? = null
    Thread {
        if (<!ESCAPING_CAPTURED_VARIABLE!>flag<!> && true) {
            print(1)
        }
        println("Hello ${<!ESCAPING_CAPTURED_VARIABLE!>name<!>}")
        if (<!ESCAPING_CAPTURED_VARIABLE!>obj<!> is String) {
            print(1)
        }
        val s = <!ESCAPING_CAPTURED_VARIABLE!>obj<!> as String
        val res = <!ESCAPING_CAPTURED_VARIABLE!>nullableStr<!> ?: "default"
    }

    Thread {
        flag = true
        name += "a"
        obj = "text"
        nullableStr = null
        println(name)
        baz(flag.toString())
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, asExpression, assignment, classDeclaration, elvisExpression,
functionDeclaration, ifExpression, integerLiteral, isExpression, javaFunction, lambdaLiteral, localProperty,
nullableType, primaryConstructor, propertyDeclaration, samConversion, stringLiteral */
