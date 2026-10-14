// RUN_PIPELINE_TILL: FRONTEND

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
        // A type variable for CSR doesn't really have no constraints and no shallow dependencies,
        // thus it cannot be resolved until the variable is fixed.
        // But on the other hand, both CLs and CSRs block type variable fixation.
        else -> id(id { <!UNRESOLVED_REFERENCE!>Y<!> })
    }

    val c: () -> (() -> SC) = { { ObjRef } }
    val d: () -> (() -> SC) = id { { Obj } }
    val e: (Int) -> ((Int) -> SC) = id(id { { Obj } })

    val f: () -> E = id(id(fun() = X))
}

/* GENERATED_FIR_TAGS: anonymousFunction, classDeclaration, companionObject, enumDeclaration, enumEntry,
functionDeclaration, functionalType, lambdaLiteral, localProperty, nestedClass, nullableType, objectDeclaration,
propertyDeclaration, sealed, typeParameter, whenExpression */
