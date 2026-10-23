// METADATA_TARGET_PLATFORMS: JVM, JS
// LANGUAGE: +AllowExpectValueClassesWithNoPrimaryConstructor +FullValueClasses
// ISSUE: KT-90593

// MODULE: lib-common
expect inline value class Val
expect inline value class ForRef(val x: Int)

// MODULE: app-common()()(lib-common)
fun fooVal(vararg vl: Val) = Unit
fun fooForRef(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> vl: ForRef) = Unit
