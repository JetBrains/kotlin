import kotlin.contracts.*

fun  <T: Throwable> throws() {}
fun  <T: Throwable> calledInTryCatch(lambda: Any) {}

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

    <!UNCHECKED_EXCEPTION!>block()<!>
    block.<!UNCHECKED_EXCEPTION!>invoke()<!>

    try {
        <!UNCHECKED_EXCEPTION!>block()<!>
        block.<!UNCHECKED_EXCEPTION!>invoke()<!>
    } catch (e: java.lang.IllegalArgumentException){ }
}
