// FIR_IDENTICAL
// SKIP_KT_DUMP
// RUN_PIPELINE_TILL: FIR2IR
// DISABLE_NEXT_PHASE_SUGGESTION: Backend flackyly fails with stack overflow error for both K1 and K2
// LANGUAGE: +EnhanceNullabilityOfPrimitiveArrays
// ^^^ To avoid flaky testing on K1, a random K2.0 language feature is used

fun test(z: Int): String {
    return "" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z +
            "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" +
            z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z +
            "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" +
            z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z +
            "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" +
            z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z +
            "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" +
            z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z +
            "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1" + z + "1"
// Note: Adding 2 more lines results in SOE in IrFileValidator on CI during FirLightTreeJvmIrTextTestGenerated
}
