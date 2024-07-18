// ISSUE: KT-69652
// WITH_EXTENDED_CHECKERS
// EXPLICIT_API_MODE: STRICT
// LANGUAGE: +DataClassCopyRespectsConstructorVisibility

public data class Location private constructor(
    <!REDUNDANT_VISIBILITY_MODIFIER!>internal<!> val ip: String,
    <!REDUNDANT_VISIBILITY_MODIFIER!>internal<!> val port: Int,
)
