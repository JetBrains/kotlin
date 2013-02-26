// "Make Object.notify open" "false"
// ERROR: 'notify' in 'Object' is final and cannot be overridden
class A : Object() {
    override<caret> fun notify() {}
}