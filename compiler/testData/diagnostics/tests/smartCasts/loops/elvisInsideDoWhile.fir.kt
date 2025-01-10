// RUN_PIPELINE_TILL: FIR2IR
// DISABLE_NEXT_PHASE_SUGGESTION: Fake override should have at least one overridden descriptor: FUN FAKE_OVERRIDE name:<get-length> visibility:public modality:OPEN <> ($this:kotlin.Nothing) returnType:kotlin.Int [fake_override]
public fun foo(x: String?): Int {
    do {
        // After the check, smart cast should work
        x ?: x!!.length
        // x is not null in both branches
        if (x.length == 0) break
    } while (true)
    return x.length
}
