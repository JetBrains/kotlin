// "Add default constructor to 'expect' class" "true"
// ENABLE_MULTIPLATFORM
// ERROR: Expected class 'A' has no actual declaration in module light_idea_test_case for JVM

expect open class A

open class C : A<caret>()
