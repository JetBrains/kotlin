// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class MutablePerson(var name: String)
class MutableObject(var mutableField: String = "initial")
class DeepObject {
    var theProblematicVar: String = "Hello"
}

class MiddleObject {
    val next: DeepObject? = DeepObject()
}

class RootObject {
    val next: MiddleObject? = MiddleObject()
}

fun baz(s: String) {
    println("baz called with: $s")
}

inline fun barInline(f: () -> Unit) {
    println("Inside barInline")
    f()
}

@OptIn(ExperimentalContracts::class)
fun barWithContract(f: () -> Unit) {
    contract {
        callsInPlace(f, InvocationKind.EXACTLY_ONCE)
    }
    println("Inside barWithContract")
    f()
}


fun barRegular(f: () -> Unit) {
    println("Inside barRegular")
    f()
}

fun processChunked(chunkHandler: (String) -> Unit) {
    chunkHandler("dataA")
    chunkHandler("dataB")
    chunkHandler("dataC")
}

fun foo() {
    var x = "bla"

    // OK: barInline is inlined.
    barInline {
        baz(x)
    }

    // OK: barWithContract promises it runs in-place exactly once.
    barWithContract {
        baz(x)
    }

    barRegular {
        baz(x)
    }

    // OK
    barRegular {
        var y = "3"
        baz(y)
    }

    val person = MutablePerson("Alice")

    barRegular {
        baz(<!IE_DIAGNOSTIC!>person.name<!>)
    }

    var sum = 0
    val numbers = listOf(1, 2, 3, 4)

    numbers.forEach {
        sum += it
    }

    barRegular {
        val localObj = MutableObject()

        println(localObj.mutableField)
    }

    var localObj = MutableObject()
    barRegular {

        println(<!IE_DIAGNOSTIC!>localObj.mutableField<!>)
    }

    val localObjVal = MutableObject()
    barRegular {

        println(<!IE_DIAGNOSTIC!>localObjVal.mutableField<!>)
    }


    barRegular {
        val root = RootObject()
        baz(root.next!!.next!!.theProblematicVar)
    }


    val root = RootObject()
    barRegular {
        baz(<!IE_DIAGNOSTIC!>root.next!!.next!!.theProblematicVar<!>)
    }

    var count = false
    barRegular {
        if (!count) {
            print(2)
        }
    }

    var accumulator = ""
    processChunked { chunk ->
        val newChunk = chunk
        val combinedData = accumulator + newChunk

        if (accumulator == "") {
            print(1)
        }
    }
}

/* GENERATED_FIR_TAGS: assignment, classReference, contractCallsEffect, contracts, functionDeclaration, functionalType,
inline, lambdaLiteral, localProperty, propertyDeclaration, stringLiteral */
