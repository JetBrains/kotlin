// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class MutablePerson(var name: String = "NoName", val age: String = "1", var child: MutablePerson = MutablePerson())
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

fun barRegularString(el : String, f: () -> Unit) {
    println("Inside barRegular")
    f()
}

fun processChunked(chunkHandler: (String) -> Unit) {
    chunkHandler("dataA")
    chunkHandler("dataB")
    chunkHandler("dataC")
}

private fun testWithClient() = barRegular {
    var clientvar = "hi"
    var another = "hello"

    barRegularString(clientvar) {
        barRegularString(another) {
            println("Hi")
        }
    }
}

var hi = "hi"

class WithMemberFunctions {
    var memberVar = "Member"
    var person = MutablePerson("Bob")

    fun testMemberCapture() {
        var personAlice = MutablePerson("Alice")

        barRegular {
            baz(memberVar)
            println(person)
            baz(<!CV_DIAGNOSTIC!>personAlice<!>.age)
        }
        personAlice = MutablePerson("Bob")
    }
}

fun exampleValuePar(userId: String) {
    barRegular {
        baz(userId)
    }
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

    var person = MutablePerson("Alice")

    barRegular {
        baz(<!CV_DIAGNOSTIC!>person<!>.name)
    }
    if (person.name != x) {
        person = MutablePerson()
    }

    barRegular {
        baz(hi)
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

    var localObjVal : MutableObject? = MutableObject()
    barRegular {
        println(<!CV_DIAGNOSTIC!>localObjVal<!>?.mutableField)
    }
    localObjVal = null

    var root : RootObject? = RootObject()
    barRegular {
        baz(<!CV_DIAGNOSTIC!>root<!>?.next!!.next!!.theProblematicVar)
        <!CV_DIAGNOSTIC!>root<!> = null
    }


    var root2 = RootObject()
    val root3 = RootObject()
    barRegular {
        baz(<!CV_DIAGNOSTIC!>root2<!>.next!!.next!!.theProblematicVar)
        <!CV_DIAGNOSTIC!>root2<!> = root3
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
        val combinedData = accumulator+newChunk

        if (accumulator == "") {
            print(1)
        }
    }

    var flag = true
    var name = "World"
    var obj: Any = "text"
    var nullableStr: String? = null
    barRegular {
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

    barRegular {
        <!CV_DIAGNOSTIC!>flag<!> = true
        <!CV_DIAGNOSTIC!>name<!> += "a"
        <!CV_DIAGNOSTIC!>obj<!> = "text"
        <!CV_DIAGNOSTIC!>nullableStr<!> = null
    }
}

fun doSomethingBig() {
    var hi = 2

    fun localHelper(message: String) {
        println(message)
        println(hi)
    }

    localHelper("1")
}


/* GENERATED_FIR_TAGS: additiveExpression, andExpression, asExpression, assignment, checkNotNullCall, classDeclaration,
classReference, contractCallsEffect, contracts, elvisExpression, equalityExpression, functionDeclaration, functionalType,
ifExpression, inline, integerLiteral, isExpression, lambdaLiteral, localProperty, nullableType, primaryConstructor,
propertyDeclaration, stringLiteral, thisExpression */
