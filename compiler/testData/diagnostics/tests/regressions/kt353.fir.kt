// RUN_PIPELINE_TILL: FRONTEND
// KT-353 Generic type argument inference sometimes doesn't work

interface A {
    fun <T> gen() : T
}

fun foo(a: A) {
    val g : () -> Unit = {
        a.gen()  //it works: Unit is derived
    }

    val u: Unit = a.gen() // Unit should be inferred

    if (true) {
        a.<!CANNOT_INFER_PARAMETER_TYPE!>gen<!>() // Shouldn't work: no info for inference
    }

    val b : () -> Unit = {
        if (true) {
            a.gen()  // unit can be inferred
        }
        else {
            Unit
        }
    }

    val f : () -> Int = {
        a.gen()  //type mismatch, but Int can be derived
    }

    a.<!CANNOT_INFER_PARAMETER_TYPE!>gen<!>() // Shouldn't work: no info for inference
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, ifExpression, interfaceDeclaration, lambdaLiteral,
localProperty, nullableType, propertyDeclaration, typeParameter */
