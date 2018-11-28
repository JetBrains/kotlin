// !API_VERSION: 1.2
// !DIAGNOSTICS: -PRE_RELEASE_CLASS, -UNUSED_PARAMETER
// !LANGUAGE: +ReleaseCoroutines
// SKIP_TXT

<!UNSUPPORTED!>suspend<!> fun dummy() {}

// TODO: Forbid
fun builder(c: <!UNSUPPORTED!>suspend<!> () -> Unit) {}

<!UNSUPPORTED!>suspend<!> fun test1() {
    kotlin.coroutines.<!UNRESOLVED_REFERENCE!>coroutineContext<!>

    kotlin.coroutines.experimental.<!UNSUPPORTED!>coroutineContext<!>

    <!UNSUPPORTED!>suspend<!> {}()

    dummy()

    val c: <!UNSUPPORTED!>suspend<!> () -> Unit = {}
    c()

    <!UNSUPPORTED!>builder<!> {}
}

fun test2() {
    kotlin.coroutines.experimental.<!DEPRECATION, UNSUPPORTED!>buildSequence<!><Int> {
        <!DEPRECATION!>yield<!>(1)
    }
    kotlin.sequences.<!UNRESOLVED_REFERENCE!>buildSequence<!><Int> {
        <!UNRESOLVED_REFERENCE!>yield<!>(1)
    }
}

<!UNSUPPORTED!>suspend<!> fun test3(): Unit = kotlin.coroutines.experimental.<!DEPRECATION!>suspendCoroutine<!> { _ -> Unit }

<!UNSUPPORTED!>suspend<!> fun test4(): Unit = kotlin.coroutines.<!UNRESOLVED_REFERENCE!>suspendCoroutine<!> { <!CANNOT_INFER_PARAMETER_TYPE!>_<!> -> Unit }
