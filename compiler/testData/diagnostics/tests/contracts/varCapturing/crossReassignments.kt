// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// FULL_JDK

fun baz(s: String) {}

private fun testReassignmentAcrossMultipleLambdas() {
    var r = 1

    Thread { r = 3 }
    Thread { r = 4 }
}

fun testReturnThread(): Thread {
    var isScheduled = false
    Thread { isScheduled = true }
    return Thread {
        if (!<!CV_DIAGNOSTIC!>isScheduled<!>) {
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
        println(<!CV_DIAGNOSTIC!>mutObj<!>.toString())
    }
}

fun testStringReassignment() {
    var x = "bla"

    Thread { x = "3" }
    Thread { println(<!CV_DIAGNOSTIC!>x<!>) }
}

fun testSmartCastReassignedInAnotherLambda() {
    var flag = true
    var name = "World"
    var obj: Any = "text"
    var nullableStr: String? = null
    Thread {
        if (<!CV_DIAGNOSTIC!>flag<!> && true) {
            print(1)
        }
        println("Hello ${<!CV_DIAGNOSTIC!>name<!>}")
        if (<!CV_DIAGNOSTIC!>obj<!> is String) {
            print(1)
        }
        val s = <!CV_DIAGNOSTIC!>obj<!> as String
        val res = <!CV_DIAGNOSTIC!>nullableStr<!> ?: "default"
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
