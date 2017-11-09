// "Change 'y' to '*y'" "false"
// ACTION: Add 'toString()' call
// ACTION: Change parameter 'x' type of function 'foo' to 'List<String>'
// ACTION: Convert to block body
// ACTION: Create function 'foo'
// DISABLE-ERRORS

// Fix this test case if https://youtrack.jetbrains.com/issue/KT-12663 is implemented

fun foo(vararg x: String) {}

fun bar(y: List<String>) = foo(y<caret>)
