// METADATA_TARGET_PLATFORMS: JVM, JS
// DIAGNOSTICS: -NOTHING_TO_INLINE

// `FirJvmInlineCheckerComponent` prohibits inlining suspend lambdas with default values.
inline suspend fun inlineWithSuspendDefault(
    <!NOT_YET_SUPPORTED_IN_INLINE!>crossinline block: suspend () -> Unit = {}<!>
) {
    block()
}
