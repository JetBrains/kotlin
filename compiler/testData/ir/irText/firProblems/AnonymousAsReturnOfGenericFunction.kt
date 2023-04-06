// FIR_IDENTICAL
// DUMP_LOCAL_DECLARATION_SIGNATURES

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57430, KT-57778

interface NestedGroupFragment

private fun addMavenOptionsGroupFragment() = addOptionsGroup<Int>()

private fun <S> addOptionsGroup() = object: NestedGroupFragment {}
