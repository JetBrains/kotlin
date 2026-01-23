// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun baz(s: String) {}

class MutablePerson(var name: String)

class WithMemberFunctions {
    var memberVar = "Member"
    var person = MutablePerson("Bob")

    fun testClassPropertiesIgnored() {
        Thread {
            baz(memberVar)
            println(person)
        }
        memberVar = "new value"
    }
}

fun testFunctionParameterIgnored(userString: String) {
    Thread {
        baz(userString)
    }
}

var hi = "hi"

fun testTopLevelPropertyIgnored() {
    Thread {
        baz(hi)
    }
    hi = "bye"
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, functionalType, lambdaLiteral,
primaryConstructor, propertyDeclaration, stringLiteral */
