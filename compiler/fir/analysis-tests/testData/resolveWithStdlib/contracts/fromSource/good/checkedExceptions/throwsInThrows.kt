import kotlin.contracts.*

fun  <T: Throwable> throws() {}
fun  <T: Throwable> calledInTryCatch(lambda: Any) {}

fun test() {
    try {
        throwsREAndDoSomething()
    } catch (e: java.lang.RuntimeException){ }

    try {
        throwsREAndDoSomething()
    } catch (e: java.lang.Exception){ }

    try {
        throwsREAndDoSomething()
    } catch (e: Throwable){ }
}

fun throwsRE() {
    contract {
        throws<java.lang.RuntimeException>()
    }

    throwsRE()
}

fun throwsREAndDoSomething() {
    contract {
        throws<java.lang.RuntimeException>()
    }

    throwsREAndDoSomething()
    throwsRE()
}

fun throwsException() {
    contract {
        throws<java.lang.Exception>()
    }

    throwsREAndDoSomething()
    throwsRE()
}

fun throwsThrowable() {
    contract {
        throws<Throwable>()
    }

    throwsException()
    throwsREAndDoSomething()
    throwsRE()
}



