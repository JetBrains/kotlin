// "Add type 'Int' to parameter 'bar'" "false"
// ERROR: A type annotation is required on a value parameter
// ACTION: Create test

class Foo(val bar<caret>)