// RUN_PIPELINE_TILL: CODEGEN
// ^^^ AbstractFirLightTreeDiagnosticsWithLatestLanguageVersionTest does not invoke Fir2IR and IR Lowerings
//     so cannot emit IR diagnostics

fun test() {
    tailrec fun() {}
}

/* GENERATED_FIR_TAGS: anonymousFunction, functionDeclaration */
