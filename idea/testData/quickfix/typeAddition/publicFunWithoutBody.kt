// "Specify return type explicitly" "false"
// ERROR: Function 'foo' without a body must be abstract
// ACTION: Add function body
// ACTION: Make 'foo' abstract
// ACTION: Convert member to extension

package a

class A() {
    public fun <caret>foo()
}