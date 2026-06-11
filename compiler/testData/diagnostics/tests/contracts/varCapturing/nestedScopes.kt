// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// FULL_JDK

fun testCaptureInsideLocalFunction() {
    var r = 2

    fun localHelper() {
        Thread {
            println(<!CV_DIAGNOSTIC!>r<!>)
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
        Thread {
            println(<!CV_DIAGNOSTIC!>l<!>)
            println(<!CV_DIAGNOSTIC!>outer<!>)
        }
        l = 2
    }
    outer = "b"
}

fun testNoWarningNestedConstructor() {
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

fun testEscapingLambdaInsideLocalConstructor() {
    var l = 2
    var r = 2

    class Local {
        constructor(i: Int) {
            var result = i + l
            Thread {
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

fun testNestedInPlaceLambdaInsideEscaping() {
    var l = 2
    Thread {
        <!CV_DIAGNOSTIC!>l<!>.let {
            println(<!CV_DIAGNOSTIC!>l<!>)
        }
    }
    l = 4
}

private fun testNestedEscapingLambdas() = Thread {
    var another = "hello"

    Thread {
        println(<!CV_DIAGNOSTIC!>another<!>)
    }

    another = "hi"
}

fun testNestedNotCapturedRead() = Thread {
    var computeCount = 0

    Thread {
        computeCount++
    }

    println(computeCount)
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classDeclaration, functionDeclaration,
incrementDecrementExpression, integerLiteral, javaFunction, lambdaLiteral, localClass, localFunction, localProperty,
propertyDeclaration, samConversion, secondaryConstructor, stringLiteral */
