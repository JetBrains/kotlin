// RUN_PIPELINE_TILL: BACKEND
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
    val a: () -> E = id(id { X })

    val b = when {
        true -> fun(): E = E.X
        else -> id(id { Y })
    }

    val c: () -> (() -> SC) = { { ObjRef } }
    val d: () -> (() -> SC) = id { { Obj } }
    val e: (Int) -> ((Int) -> SC) = id(id { { Obj } })

    val f: () -> E = id(id(fun() = X))
}

/* GENERATED_FIR_TAGS: anonymousFunction, classDeclaration, companionObject, enumDeclaration, enumEntry,
functionDeclaration, functionalType, lambdaLiteral, localProperty, nestedClass, nullableType, objectDeclaration,
propertyDeclaration, sealed, typeParameter, whenExpression */
