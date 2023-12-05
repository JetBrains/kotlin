// DIAGNOSTICS: -UNCHECKED_CAST
// This test is a copy of conversionLastStatementInLambda, but with generic functions
// However works diffirently in K1

fun main(b: Boolean) {
    callWithLambda {
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::test1<!>
    }

    callWithLambda {
        if (b) <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::test1<!> else <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::test2<!>
    }

    callWithLambda {
        if (b) {
            ::test1
        } else {
            ::test2
        }
    }

    callWithLambda {
        (<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::test1<!>)
    }
}

fun <T> test1(): T = "" as T
fun <T> test2(): T = "" as T

fun callWithLambda(action: () -> () -> Unit) {}
