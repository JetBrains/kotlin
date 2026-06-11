// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// FULL_JDK

fun testDirectReassignment() {
    var unstable = ""
    Thread {
        println(<!CV_DIAGNOSTIC!>unstable<!>)
    }
    unstable = "hello"
}

class MutablePerson(var name: String = "NoName")
fun baz(s: String) {}

fun testConditionalObjectReassignment(x : String) {
    var person = MutablePerson("Alice")

    Thread {
        baz(<!CV_DIAGNOSTIC!>person<!>.name)
    }
    if (person.name != x) {
        person = MutablePerson()
    }
}

class MutableObject(var mutableField: String = "initial")

fun testNullableVariableReassignment() {
    var localObjVal : MutableObject? = MutableObject()
    Thread {
        println(<!CV_DIAGNOSTIC!>localObjVal<!>?.mutableField)
    }
    localObjVal = null
}

private fun testReassignmentAfterNestedCapture(){
    var first = true
    Thread {
        Thread {
            if (<!CV_DIAGNOSTIC!>first<!>) {
                first = false
            }
        }
    }
    first = true
}

class DeepObject { var theProblematicVar: String = "Hello" }

class MiddleObject { val next: DeepObject? = DeepObject() }

class RootObject { val next: MiddleObject? = MiddleObject() }

fun testNullableObjectReassignment() {
    var root : RootObject? = RootObject()
    Thread {
        baz(<!CV_DIAGNOSTIC!>root<!>?.next!!.next!!.theProblematicVar)
    }
    root = null
}

fun testObjectReferenceReassignment() {
    var root2 = RootObject()
    val root3 = RootObject()
    Thread {
        baz(<!CV_DIAGNOSTIC!>root2<!>.next!!.next!!.theProblematicVar)
    }
    root2 = root3
}

/* GENERATED_FIR_TAGS: assignment, checkNotNullCall, classDeclaration, equalityExpression, functionDeclaration,
ifExpression, javaFunction, lambdaLiteral, localProperty, nullableType, primaryConstructor, propertyDeclaration,
safeCall, samConversion, stringLiteral */
