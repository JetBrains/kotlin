// WITH_STDLIB
// LANGUAGE: -ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
@kotlin.ConsistentCopyVisibility
class Foo

@kotlin.ExposedCopyVisibility
class Bar

@kotlin.ConsistentCopyVisibility
data class DataA(val x: Int)

@kotlin.ExposedCopyVisibility
data class DataB(val x: Int)
