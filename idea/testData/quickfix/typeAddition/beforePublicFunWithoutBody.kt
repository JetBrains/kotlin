// "Specify return type explicitly" "false"
// ERROR: Function 'foo' without a body must be abstract
// ACTION: Add function body
// ACTION: Make 'foo' abstract
// ACTION: Convert to extension
// ACTION: Disable 'Convert to extension'
// ACTION: Edit intention settings

package a

class A() {
    public fun <caret>foo()
}