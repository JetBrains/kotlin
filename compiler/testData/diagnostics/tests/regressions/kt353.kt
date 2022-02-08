// FIR_IDENTICAL
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
        a.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>gen<!>() // Shouldn't work: no info for inference
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

    a.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>gen<!>() // Shouldn't work: no info for inference
}
