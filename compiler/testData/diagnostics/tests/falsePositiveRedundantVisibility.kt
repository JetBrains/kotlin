// FIR_IDENTICAL
// ISSUE: KT-69652
// WITH_EXTRA_CHECKERS
// EXPLICIT_API_MODE: STRICT
// LANGUAGE: +DataClassCopyRespectsConstructorVisibility

public data class Location private constructor(
    internal val ip: String,
    internal val port: Int,
)
