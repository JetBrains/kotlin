// WITH_STDLIB
// LANGUAGE: +DataClassCopyRespectsConstructorVisibility
<!REDUNDANT_ANNOTATION!>@kotlin.ConsistentDataCopyVisibility<!>
data class Data private constructor(val x: Int)
