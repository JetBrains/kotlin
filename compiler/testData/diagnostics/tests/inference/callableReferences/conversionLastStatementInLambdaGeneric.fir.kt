// DIAGNOSTICS: -UNCHECKED_CAST
// This test is a copy of conversionLastStatementInLambda, but with generic functions
// However works diffirently in K1

fun main(b: Boolean) {
    callWithLambda {
        ::test1
    }

    callWithLambda {
        if (b) ::test1 else ::test2
    }

    callWithLambda {
        if (b) {
            ::test1
        } else {
            ::test2
        }
    }

    callWithLambda {
        (::test1)
    }
}

fun <T> test1(): T = "" as T
fun <T> test2(): T = "" as T

fun callWithLambda(action: () -> () -> Unit) {}