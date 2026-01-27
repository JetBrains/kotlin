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
        val age = <!CV_DIAGNOSTIC!>wrapper<!>.age
        println(age)

        var ageVar = <!CV_DIAGNOSTIC!>wrapper<!>.age
        ageVar + 3
    }

    wrapper = MutablePerson(age = "100")
    curLambda()
}

fun testPrimitiveSnapshotIsSafe() {
    var personNoName = MutablePerson()

    barRegular {
        val personAlice = MutablePerson(name = "Alice")
        personAlice.name = <!CV_DIAGNOSTIC!><!CV_DIAGNOSTIC!>personNoName<!>.name<!>
        println(personAlice.name) // mistake

        var namePerson = <!CV_DIAGNOSTIC!><!CV_DIAGNOSTIC!>personNoName<!>.name<!>
        namePerson
    }
}

fun testObjectAliasIsUnsafe() {
    var girl = MutablePerson(name = "Alice")
    barRegular {
        var boy = <!CV_DIAGNOSTIC!>girl<!>
        <!CV_DIAGNOSTIC!>boy.name = "bob"<!> // mistake
        println(boy.age)
        println(<!CV_DIAGNOSTIC!>boy.name<!>)

        val localChild = <!CV_DIAGNOSTIC!><!CV_DIAGNOSTIC!>girl<!>.child<!>
        if (localChild != null) {
            println(<!CV_DIAGNOSTIC!>localChild.name<!>)
        }

        val immutableBoy = <!CV_DIAGNOSTIC!>girl<!>
        immutableBoy.age + <!CV_DIAGNOSTIC!>immutableBoy.name<!>
    }
}

fun testReassignAlias() {
    var girl = MutablePerson(name = "Alice")
    barRegular {
        var boy = <!CV_DIAGNOSTIC!>girl<!>
        println(<!CV_DIAGNOSTIC!>boy.name<!>)

        boy = MutablePerson(name = "Boy")
        println(boy.name)
    }
}

fun testRExpressionWithCaptured() {
    var girl = MutablePerson(name = "Alice")
    barRegular {
        val boyFun = MutablePerson()

        if (<!CV_DIAGNOSTIC!><!CV_DIAGNOSTIC!>girl<!>.child<!> != null) {
            boyFun.child = <!CV_DIAGNOSTIC!><!CV_DIAGNOSTIC!>girl<!>.child<!>
            println(boyFun.child?.name) // mistake
            println(boyFun.child) // mistake
        }
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, functionalType, lambdaLiteral, localProperty,
primaryConstructor, propertyDeclaration, stringLiteral */
