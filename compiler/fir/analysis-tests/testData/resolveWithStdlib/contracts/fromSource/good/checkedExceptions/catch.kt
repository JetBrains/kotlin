import kotlin.contracts.*

fun  <T: Throwable> throws() {}
fun  <T: Throwable> calledInTryCatch(lambda: Any) {}

fun test() {
    try {
        throwsRE()
    } catch (e: java.lang.RuntimeException){ }

    try {
        throwsRE()
    } catch (e: java.lang.Exception){ }

    try {
        throwsRE()
    } catch (e: Throwable){ }
}

fun throwsRE() {
    contract {
        throws<java.lang.RuntimeException>()
    }

    throwsRE()
}


