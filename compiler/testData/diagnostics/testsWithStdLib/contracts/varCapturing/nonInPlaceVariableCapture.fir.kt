// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class MutablePerson(var name: String = "NoName", val age: String = "1", var child: MutablePerson? = null)
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
        barRegularString(<!CV_DIAGNOSTIC!>another<!>) {
            println("Hi")
        }
    }
}

var hi = "hi"

class WithMemberFunctions {
    var memberVar = "Member"
    var person = MutablePerson("Bob")

    fun testMemberCapture() {
        val personAlice = MutablePerson("Alice")

        barRegular {
            baz(memberVar)
            baz(person.name)
            baz(person.age)
            baz(this.memberVar)
            baz(<!CV_DIAGNOSTIC!>personAlice.name<!>)
            baz(personAlice.age)
            println(<!CV_DIAGNOSTIC!>personAlice.child<!>?.age)
        }
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
        baz(<!CV_DIAGNOSTIC!>x<!>)
    }

    // OK
    barRegular {
        var unstableY = "3"
        baz(unstableY)
    }

    barRegular {
        var unstableY2 = false
        if (!unstableY2) {
            println("hi")
        }
    }

    var badSink : Int? = 42
    barRegular {
        <!CV_DIAGNOSTIC!>badSink = null<!>
    }


    val person = MutablePerson("Alice")

    barRegular {
        baz(<!CV_DIAGNOSTIC!>person.name<!>)
    }

    var person2 = MutablePerson("Alice")

    barRegular {
        baz(<!CV_DIAGNOSTIC!>person2<!>.age)
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

    var localObj = MutableObject()
    barRegular {

        println(<!CV_DIAGNOSTIC!><!CV_DIAGNOSTIC!>localObj<!>.mutableField<!>)
    }

    val localObjVal = MutableObject()
    barRegular {
        println(<!CV_DIAGNOSTIC!>localObjVal.mutableField<!>)
    }

    barRegular {
        val root = RootObject()
        baz(root.next!!.next!!.theProblematicVar)
    }


    val root = RootObject()
    barRegular {
        baz(<!CV_DIAGNOSTIC!>root.next!!.next!!.theProblematicVar<!>)
    }

    var count = false
    barRegular {
            if (!<!CV_DIAGNOSTIC!>count<!>) {
            print(2)
        }
    }
    var accumulator = ""
    processChunked { chunk ->
        val newChunk = chunk
        val combinedData = <!CV_DIAGNOSTIC!>accumulator<!>+newChunk

        if (<!CV_DIAGNOSTIC!>accumulator<!> == "") {
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

}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, asExpression, assignment, checkNotNullCall, classDeclaration,
classReference, contractCallsEffect, contracts, elvisExpression, equalityExpression, functionDeclaration, functionalType,
ifExpression, inline, integerLiteral, isExpression, lambdaLiteral, localProperty, nullableType, primaryConstructor,
propertyDeclaration, stringLiteral, thisExpression */
