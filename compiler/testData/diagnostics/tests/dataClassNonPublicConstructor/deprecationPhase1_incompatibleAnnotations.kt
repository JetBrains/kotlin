// WITH_STDLIB
// LANGUAGE: -ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
@kotlin.ConsistentDataCopyVisibility
@kotlin.InconsistentDataCopyVisibility
data class Data private constructor(val x: Int)
