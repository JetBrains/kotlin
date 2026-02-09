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

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, functionalType, lambdaLiteral, localProperty,
propertyDeclaration, stringLiteral */
