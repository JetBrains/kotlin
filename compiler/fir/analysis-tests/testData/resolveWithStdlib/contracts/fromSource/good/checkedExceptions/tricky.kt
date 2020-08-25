import kotlin.contracts.*

fun  <T: Throwable> throws() {}
fun  <T: Throwable> calledInTryCatch(lambda: Any) {}

fun test() {
    try {
        throwsRE()

        try {
            throwsRE()
        } catch (e: java.lang.RuntimeException){ }

        throwsRE()

        catchRE {
            throwsRE()
        }

        throwsRE()

        try {
            throwsRE()
        } catch (e: java.lang.IllegalStateException){ }

        throwsRE()
    } catch (e: java.lang.RuntimeException){ }
}

fun throwsRE() {
    contract {
        throws<java.lang.RuntimeException>()
    }

    throwsRE()
}


fun catchRE(block: () -> Unit){
    contract {
        calledInTryCatch<java.lang.RuntimeException>(block)
        callsInPlace(block, InvocationKind.UNKNOWN)
    }

    try {
        block()
    } catch (e: java.lang.RuntimeException){ }
}


