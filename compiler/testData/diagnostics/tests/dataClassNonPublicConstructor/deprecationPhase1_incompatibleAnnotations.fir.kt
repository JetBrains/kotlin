// WITH_STDLIB
// LANGUAGE: -ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
<!DATA_CLASS_CONSISTENT_COPY_AND_EXPOSED_COPY_ARE_INCOMPATIBLE_ANNOTATIONS!>@kotlin.ConsistentCopyVisibility<!>
<!DATA_CLASS_CONSISTENT_COPY_AND_EXPOSED_COPY_ARE_INCOMPATIBLE_ANNOTATIONS!>@kotlin.ExposedCopyVisibility<!>
data class Data private constructor(val x: Int)
