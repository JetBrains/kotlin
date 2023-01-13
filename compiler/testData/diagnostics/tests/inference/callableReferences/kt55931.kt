// ISSUE: KT-55931
// SKIP_TXT

fun fun1() {}
fun fun2() {}

fun takeLambda(lambda: () -> Unit) = lambda()

fun foo(b: Boolean) {
    val x1 = if (b) { ::fun1 } else { ::fun2 } // OK
    takeLambda {
        val x2 = if (b) ::fun1 else ::fun2 // OK
        // NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER and DEBUG_UNRESOLVED on both callable references
        // Since 1.4.0 (NI)
        val x3 = if (b) { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>fun1<!><!> } else { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>fun2<!><!> }
    }

    val w: () -> Unit = {
        val x4 = if (b) ::fun1 else ::fun2 // OK
        // OK, too
        val x5 = if (b) { ::fun1 } else { ::fun2 }
    }
}
