// WITH_STDLIB
// LANGUAGE: -ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
<!DATA_CLASS_CONSISTENT_COPY_WRONG_ANNOTATION_TARGET!>@kotlin.ConsistentCopyVisibility<!>
class Foo

<!DATA_CLASS_CONSISTENT_COPY_WRONG_ANNOTATION_TARGET!>@kotlin.ExposedCopyVisibility<!>
class Bar

<!REDUNDANT_ANNOTATION!>@kotlin.ConsistentCopyVisibility<!>
data class DataA(val x: Int)

<!REDUNDANT_ANNOTATION!>@kotlin.ExposedCopyVisibility<!>
data class DataB(val x: Int)
