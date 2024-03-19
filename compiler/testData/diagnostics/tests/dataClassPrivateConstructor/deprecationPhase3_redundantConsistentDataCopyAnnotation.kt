// WITH_STDLIB
// LANGUAGE: +DataClassCopyRespectsConstructorVisibility
@kotlin.ConsistentDataCopyVisibility
data class Data private constructor(val x: Int)
