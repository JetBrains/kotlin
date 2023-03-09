// FIR_IDENTICAL

// NO_SIGNATURE_DUMP
// ^KT-57430

interface NestedGroupFragment

private fun addMavenOptionsGroupFragment() = addOptionsGroup<Int>()

private fun <S> addOptionsGroup() = object: NestedGroupFragment {}
