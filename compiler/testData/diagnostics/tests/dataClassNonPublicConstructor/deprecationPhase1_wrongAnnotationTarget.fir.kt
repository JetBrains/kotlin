// WITH_STDLIB
// LANGUAGE: -ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
<!DATA_CLASS_CONSISTENT_COPY_WRONG_ANNOTATION_TARGET!>@kotlin.ConsistentCopyVisibility<!>
class Foo

<!DATA_CLASS_CONSISTENT_COPY_WRONG_ANNOTATION_TARGET!>@kotlin.ExposedCopyVisibility<!>
class Bar
