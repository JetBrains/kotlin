// "Add parameter to constructor 'Foo'" "true"
// WITH_RUNTIME
// DISABLE-ERRORS
private val foo = Foo(1, "2", <caret>setOf("3"))