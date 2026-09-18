// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
// LATEST_LV_DIFFERENCE
// ^^^ AbstractFirLightTreeDiagnosticsWithLatestLanguageVersionTest does not invoke Fir2IR and IR Lowerings
//     so cannot emit IR diagnostics
val t2 = context(a: String) <!NO_TAIL_CALLS_FOUND!>tailrec<!> fun() {}

/* GENERATED_FIR_TAGS: anonymousFunction, propertyDeclaration */
