// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK
// IDE_MODE
// ISSUE: KT-85341

object Constants {
    val STATE = "state"
}

class A(val x: String)

class B(val a: A)

class C(val d: D)

private val Anonymous = object {
    val State: A = A(Constants.STATE)
    val AnotherState: C = C(D1.ANOTHER_STATE)
}

class D {
    companion object {
        val ANOTHER_STATE = Anonymous.AnotherState
    }
}

object D1 {
    val ANOTHER_STATE: D = D()
}

private val STATE = B(Anonymous.State)
/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, companionObject, objectDeclaration,
primaryConstructor, propertyDeclaration, stringLiteral */
