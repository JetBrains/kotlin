// WITH_STDLIB
// LANGUAGE: -ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
<!DATA_CLASS_CONSISTENT_COPY_WRONG_ANNOTATION_TARGET!>@kotlin.ConsistentDataCopyVisibility<!>
class Foo

<!DATA_CLASS_CONSISTENT_COPY_WRONG_ANNOTATION_TARGET!>@kotlin.InconsistentDataCopyVisibility<!>
class Bar

<!REDUNDANT_ANNOTATION!>@kotlin.ConsistentDataCopyVisibility<!>
data class Data(val x: Int)
