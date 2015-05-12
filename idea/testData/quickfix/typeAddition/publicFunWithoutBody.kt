// "Specify return type explicitly" "false"
// ERROR: Function 'foo' without a body must be abstract
// ACTION: Add function body
// ACTION: Make 'foo' abstract
// ACTION: Convert member to extension
// ACTION: Disable 'Convert to extension'
// ACTION: Disable inspection
// ACTION: Disable inspection
// ACTION: Edit inspection profile setting
// ACTION: Edit inspection profile setting
// ACTION: Edit intention settings

package a

class A() {
    public fun <caret>foo()
}