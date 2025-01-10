// RUN_PIPELINE_TILL: FIR2IR
// DISABLE_NEXT_PHASE_SUGGESTION: Fake override should have at least one overridden descriptor: FUN FAKE_OVERRIDE name:toString visibility:public modality:OPEN <> ($this:kotlin.Nothing) returnType:kotlin.String [fake_override]
fun <T> test(t: T): T {
    if (t != null) {
        return t<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    }
    return <!ALWAYS_NULL!>t<!>!!
}

fun <T> T.testThis(): String {
    if (this != null) {
        return this<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.toString()
    }
    return this!!.toString()
}

