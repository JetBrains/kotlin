import kotlin.contracts.*

fun  <T: Throwable> throws() {}
fun  <T: Throwable> calledInTryCatch(lambda: Any) {}

fun test() {
    catchRE {
        throwsRE()
    }

    catchRE {

        throwsRE()

        catchRE {
            throwsRE()
        }

        throwsRE()
    }
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
        block.invoke()
    } catch (e: java.lang.RuntimeException){ }
}
