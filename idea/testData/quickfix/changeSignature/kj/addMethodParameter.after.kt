// "Add parameter to function 'bar'" "true"
// WITH_RUNTIME
// DISABLE-ERRORS
private val foo = Foo().bar(1, "2", <caret>setOf("3"))