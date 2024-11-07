// FIR_IDENTICAL
// ISSUE: KT-72618
// DIAGNOSTICS: -EXTENSION_SHADOWED_BY_MEMBER

class MatchSticksInc {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun String.inc() = this + "|"
}

class MatchSticksPlus {
    operator fun String.plus(s: String) = this + "|"
}
