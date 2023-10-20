// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt
expect annotation class Ann()

@Ann
expect class MatchUseSameName

@Ann
expect class MatchUseTypealiasedName

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
annotation class AnnImpl
actual typealias Ann = AnnImpl

@Ann
actual class MatchUseSameName

@AnnImpl
actual class MatchUseTypealiasedName
