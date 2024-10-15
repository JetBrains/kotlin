// RUN_PIPELINE_TILL: SOURCE
// WITH_STDLIB
// LANGUAGE: -ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
@kotlin.ConsistentCopyVisibility
@kotlin.ExposedCopyVisibility
data class Data private constructor(val x: Int)
