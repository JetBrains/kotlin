// FIR_IDENTICAL
interface NestedGroupFragment

private fun addMavenOptionsGroupFragment() = addOptionsGroup<Int>()

private fun <S> addOptionsGroup() = object: NestedGroupFragment {}
