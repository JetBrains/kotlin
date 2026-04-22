// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class E {
    X, Y
}

sealed class SC {
    object Obj : SC()

    companion object {
        val ObjRef = Obj
    }
}

fun <T> id(t: T): T = t

fun test() {
    val a: () -> E = id(id { <!UNRESOLVED_REFERENCE!>X<!> })

    val b = when {
        true -> fun(): E = E.X
        else -> id(id { <!UNRESOLVED_REFERENCE!>Y<!> })
    }

    val c: () -> (() -> SC) = { { ObjRef } }
    val d: () -> (() -> SC) = id { { Obj } }
    val e: (Int) -> ((Int) -> SC) = id(id { { <!UNRESOLVED_REFERENCE!>Obj<!> } })

    val f: () -> E = id(id(fun() = <!UNRESOLVED_REFERENCE!>X<!>))
}

/* GENERATED_FIR_TAGS: anonymousFunction, classDeclaration, companionObject, enumDeclaration, enumEntry,
functionDeclaration, functionalType, lambdaLiteral, localProperty, nestedClass, nullableType, objectDeclaration,
propertyDeclaration, sealed, typeParameter, whenExpression */
