// "Make Object.notify open" "false"
// ERROR: 'notify' in 'Object' is final and cannot be overridden
// ACTION: Convert to extension
// ACTION: Disable 'Convert to extension'
// ACTION: Edit intention settings
class A : Object() {
    override<caret> fun notify() {}
}