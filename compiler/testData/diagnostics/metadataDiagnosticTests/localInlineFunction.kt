// METADATA_TARGET_PLATFORMS: JVM, JS
// DIAGNOSTICS: -NOTHING_TO_INLINE

// `FirJvmInlineCheckerComponent` prohibits local inline functions.
fun test() {
    <!NOT_YET_SUPPORTED_LOCAL_INLINE_FUNCTION!>inline<!> fun localInline() = 42
    localInline()
}
