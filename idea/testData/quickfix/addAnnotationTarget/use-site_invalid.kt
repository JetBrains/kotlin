// "Add annotation target" "false"
// ACTION: Create test
// ACTION: Make internal
// ACTION: Make private
// ERROR: This annotation is not applicable to target 'class' and use site target '@get'
annotation class Ann

<caret>@get:Ann
class Test(val foo: String)