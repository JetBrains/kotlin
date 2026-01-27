// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun barRegular(f: () -> Unit) {
    f()
}

fun <T> id(x: T): T = x

class MutablePerson(var name: String = "NoName", val age: String = "1", var child: MutablePerson? = null)

fun testStringSnapshotIsSafe() {
    var wrapper = MutablePerson()

    val curLambda = {
        val age = wrapper.age
        println(age)

        var ageVar = wrapper.age
        ageVar + 3
    }

    wrapper = MutablePerson(age = "100")
    curLambda()
}

fun testPrimitiveSnapshotIsSafe() {
    var personNoName = MutablePerson()

    barRegular {
        val personAlice = MutablePerson(name = "Alice")
        personAlice.name = personNoName.name
        println(personAlice.name)

        var namePerson = personNoName.name
        namePerson
    }
}

fun testObjectAliasIsUnsafe() {
    var girl = MutablePerson(name = "Alice")
    barRegular {
        var boy = girl
        boy.name = "bob"
        println(boy.age)
        println(boy.name)

        val localChild = girl.child
        if (localChild != null) {
            println(<!DEBUG_INFO_SMARTCAST!>localChild<!>.name)
        }

        val immutableBoy = girl
        immutableBoy.age + immutableBoy.name
    }
}

fun testReassignAlias() {
    var girl = MutablePerson(name = "Alice")
    barRegular {
        var boy = girl
        println(boy.name)

        boy = MutablePerson(name = "Boy")
        println(boy.name)
    }
}

fun testRExpressionWithCaptured() {
    var girl = MutablePerson(name = "Alice")
    barRegular {
        val boyFun = MutablePerson()

        if (girl.child != null) {
            boyFun.child = girl.child
            println(boyFun.child?.name)
            println(boyFun.child)
        }
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, functionalType, lambdaLiteral, localProperty,
primaryConstructor, propertyDeclaration, stringLiteral */
