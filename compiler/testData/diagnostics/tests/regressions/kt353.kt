// KT-353 Generic type argument inference sometimes doesn't work

trait A {
    fun <T> gen() : T
}

fun foo(a: A) {
    val <!UNUSED_VARIABLE!>g<!> : () -> Unit = {
        a.gen()  //it works: Unit is derived
    }

    val <!UNUSED_VARIABLE!>u<!>: Unit = a.gen() // Unit should be inferred

    if (true) {
        a.<!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>gen<!>() // Shouldn't work: no info for inference
    }

    val <!UNUSED_VARIABLE!>b<!> : () -> Unit = {
        if (true) {
            a.gen()  // unit can be inferred
        }
        else {
            Unit
        }
    }

    val <!UNUSED_VARIABLE!>f<!> : () -> Int = { () : Int ->
        a.gen()  //type mismatch, but Int can be derived
    }

    a.<!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>gen<!>() // Shouldn't work: no info for inference
}
