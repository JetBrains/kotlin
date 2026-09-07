// RUN_PIPELINE_TILL: CODEGEN
// LATEST_LV_DIFFERENCE
// ^^^ AbstractFirLightTreeDiagnosticsWithLatestLanguageVersionTest does not invoke Fir2IR and IR Lowerings
//     so cannot emit IR diagnostics

fun test() {
    <!NO_TAIL_CALLS_FOUND!>tailrec<!> fun() {}
}

/* GENERATED_FIR_TAGS: anonymousFunction, functionDeclaration */
