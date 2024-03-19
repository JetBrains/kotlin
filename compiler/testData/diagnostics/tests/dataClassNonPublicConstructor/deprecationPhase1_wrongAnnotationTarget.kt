// WITH_STDLIB
// LANGUAGE: -ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
@kotlin.ConsistentCopyVisibility
class Foo

@kotlin.ExposedCopyVisibility
class Bar
