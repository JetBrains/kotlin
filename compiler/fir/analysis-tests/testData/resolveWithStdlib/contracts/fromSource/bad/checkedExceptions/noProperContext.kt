import kotlin.contracts.*

fun  <T: Throwable> throws() {}
fun  <T: Throwable> calledInTryCatch(lambda: Any) {}

fun test() {
    <!UNCHECKED_EXCEPTION!>throwsRE()<!>

    try {
        throwsRE()
    } catch (e: java.lang.RuntimeException){ }

    try {
        <!UNCHECKED_EXCEPTION!>throwsRE()<!>

        try {
            throwsRE()
        } catch (e: java.lang.RuntimeException){ }

        try {
            <!UNCHECKED_EXCEPTION!>throwsRE()<!>
        } catch (e: java.lang.IllegalStateException){ }

    } catch (e: java.lang.IllegalStateException){ }

    <!UNCHECKED_EXCEPTION!>throwsRE()<!>
}

fun throwsRE() {
    contract {
        throws<java.lang.RuntimeException>()
    }

    throwsRE()
}


