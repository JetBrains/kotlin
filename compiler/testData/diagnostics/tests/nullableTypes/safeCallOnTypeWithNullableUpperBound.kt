// RUN_PIPELINE_TILL: FIR2IR
// DISABLE_NEXT_PHASE_SUGGESTION: Fake override should have at least one overridden descriptor: FUN FAKE_OVERRIDE name:toString visibility:public modality:OPEN <> ($this:kotlin.Nothing) returnType:kotlin.String [fake_override]
fun <T> test(t: T): String? {
    if (t != null) {
        return t<!UNNECESSARY_SAFE_CALL!>?.<!>toString()
    }
    return <!DEBUG_INFO_CONSTANT!>t<!>?.toString()
}

fun <T> T.testThis(): String? {
    if (this != null) {
        return this<!UNNECESSARY_SAFE_CALL!>?.<!>toString()
    }
    return this?.toString()
}
