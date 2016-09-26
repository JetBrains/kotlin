// "Add type 'Int' to parameter 'bar'" "false"
// ERROR: A type annotation is required on a value parameter
// ACTION: Create test
// ACTION: Convert to secondary constructor

class Foo(val bar<caret>)