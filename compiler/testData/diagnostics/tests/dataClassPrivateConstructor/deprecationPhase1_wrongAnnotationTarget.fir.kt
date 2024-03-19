// WITH_STDLIB
// LANGUAGE: +DataClassCopyRespectsConstructorVisibility
<!DATA_CLASS_CONSISTENT_COPY_WRONG_ANNOTATION_TARGET!>@kotlin.ConsistentDataCopyVisibility<!>
class Foo

<!DATA_CLASS_CONSISTENT_COPY_WRONG_ANNOTATION_TARGET!>@kotlin.InconsistentDataCopyVisibility<!>
class Bar
