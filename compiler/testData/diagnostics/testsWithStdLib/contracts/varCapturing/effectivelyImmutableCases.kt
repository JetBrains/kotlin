// RUN_PIPELINE_TILL: BACKEND
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun barRegular(f: () -> Unit) {}

fun baz(s: String) {}

private fun testStable() = barRegular {
    var another = "hello"

    barRegular {
        println(another)
    }
}

private fun testUnstable() = barRegular {
    var another = "hello"

    barRegular {
        println(another)
    }

    another = "hi"
}

private fun testNotCaptured() {
    barRegular {
        var another = "hello"
        println(another)
    }
}

private fun testUnstableNotCaptured() {
    barRegular {
        var isEmpty = true
        barRegular {
            isEmpty = false
        }
        if (isEmpty) {
            println("Empty")
        }
    }
}

private fun testSimpleCapturedCase(){
    var first = true
    barRegular {
        barRegular {
            if (first) {
                first = false
            }
        }
    }
}

fun testReturnAnonymousFunction(): (String) -> Unit {
    var isScheduled = false
    return { t ->
        if (!isScheduled) {
            isScheduled = true
            barRegular {
                baz(t)
                isScheduled = false
            }
        }
    }
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, functionalType, lambdaLiteral, localProperty,
propertyDeclaration, stringLiteral */
