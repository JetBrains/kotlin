// LANGUAGE: +IrIntraModuleInlinerBeforeKlibSerialization +IrCrossModuleInlinerBeforeKlibSerialization
// RUN_PIPELINE_TILL: FRONTEND
// FIR_DIFFERENCE
// The diagnostic cannot be implemented with the FIR frontend checker because it requires constant evaluation over FIR.
// The diagnostic is implemented as a klib check over IR.

// DIAGNOSTICS: -UNUSED_PARAMETER
fun Int.foo(x: Int) {
    js(<!JSCODE_ERROR!>"this = x;"<!>)
}
