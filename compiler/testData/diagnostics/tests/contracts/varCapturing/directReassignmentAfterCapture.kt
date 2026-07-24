// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// FULL_JDK
// WITH_EXTRA_CHECKERS
// LANGUAGE: +ReportEscapingCapturedVariable

fun testDirectReassignment() {
    var unstable = ""
    Thread {
        println(<!ESCAPING_CAPTURED_VARIABLE!>unstable<!>)
    }
    unstable = "hello"
}

class MutablePerson(var name: String = "NoName")
fun baz(s: String) {}

fun testConditionalObjectReassignment(x : String) {
    var person = MutablePerson("Alice")

    Thread {
        baz(<!ESCAPING_CAPTURED_VARIABLE!>person<!>.name)
    }
    if (person.name != x) {
        person = MutablePerson()
    }
}

class MutableObject(var mutableField: String = "initial")

fun testNullableVariableReassignment() {
    var localObjVal : MutableObject? = MutableObject()
    Thread {
        println(<!ESCAPING_CAPTURED_VARIABLE!>localObjVal<!>?.mutableField)
    }
    localObjVal = null
}

private fun testReassignmentAfterNestedCapture(){
    var first = true
    Thread {
        Thread {
            if (<!ESCAPING_CAPTURED_VARIABLE!>first<!>) {
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
        baz(<!ESCAPING_CAPTURED_VARIABLE!>root<!>?.next!!.next!!.theProblematicVar)
    }
    root = null
}

fun testObjectReferenceReassignment() {
    var root2 = RootObject()
    val root3 = RootObject()
    Thread {
        baz(<!ESCAPING_CAPTURED_VARIABLE!>root2<!>.next!!.next!!.theProblematicVar)
    }
    root2 = root3
}

/* GENERATED_FIR_TAGS: assignment, checkNotNullCall, classDeclaration, equalityExpression, functionDeclaration,
ifExpression, javaFunction, lambdaLiteral, localProperty, nullableType, primaryConstructor, propertyDeclaration,
safeCall, samConversion, stringLiteral */
