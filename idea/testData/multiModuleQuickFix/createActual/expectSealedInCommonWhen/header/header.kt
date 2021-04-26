// "Add remaining branches" "true"
// SHOULD_BE_AVAILABLE_AFTER_EXECUTION
// ERROR: Expected class 'CommonSealedClass' has no actual declaration in module testModule_Common
// ERROR: Unresolved reference: TODO
// ERROR: Unresolved reference: TODO
// ERROR: Unresolved reference: TODO

expect sealed class CommonSealedClass()
class SInheritor1 : CommonSealedClass()
class SInheritor2 : CommonSealedClass()

fun hello(c: CommonSealedClass): Int = <caret>when(c) {

}