import kotlin.contracts.*

fun  <T: Throwable> throws() {}
fun  <T: Throwable> calledInTryCatch(lambda: Any) {}

fun test() {
    catchRENotInPlace {
        <!UNCHECKED_EXCEPTION!>throwsRE()<!>
    }
}

fun throwsRE() {
    contract {
        throws<java.lang.RuntimeException>()
    }

    throwsRE()
}

fun catchRENotInPlace(block: () -> Unit){
    contract {
        calledInTryCatch<java.lang.RuntimeException>(block)
    }

    <!UNCHECKED_EXCEPTION!>block()<!>
    block.<!UNCHECKED_EXCEPTION!>invoke()<!>

    try {
        <!UNCHECKED_EXCEPTION!>block()<!>
        block.<!UNCHECKED_EXCEPTION!>invoke()<!>
    } catch (e: java.lang.IllegalArgumentException){ }

    try {
        block()
        block.invoke()
    } catch (e: java.lang.RuntimeException){ }
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
