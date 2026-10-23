// LL_FIR_DIVERGENCE
//   AA tests treat lib-common as source dependency
// LL_FIR_DIVERGENCE
// METADATA_TARGET_PLATFORMS: JVM, JS
// LANGUAGE: +AllowExpectValueClassesWithNoPrimaryConstructor +FullValueClasses
// ISSUE: KT-90593

// MODULE: lib-common
expect inline value class Val
expect inline value class ForRef(val x: Int)

// MODULE: app-common()()(lib-common)
fun fooVal(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> vl: Val) = Unit
fun fooForRef(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> vl: ForRef) = Unit
