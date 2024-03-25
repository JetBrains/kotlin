// WITH_STDLIB
// LANGUAGE: -ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
<!DATA_CLASS_CONSISTENT_COPY_AND_INCONSISTENT_COPY_ARE_INCOMPATIBLE_ANNOTATIONS!>@kotlin.ConsistentDataCopyVisibility<!>
<!DATA_CLASS_CONSISTENT_COPY_AND_INCONSISTENT_COPY_ARE_INCOMPATIBLE_ANNOTATIONS!>@kotlin.InconsistentDataCopyVisibility<!>
data class Data private constructor(val x: Int)
