// "Make overridden member in supertype open" "false"
// ERROR: 'notify' in 'Object' is final and cannot be overridden
class E : Object() {
    override<caret> fun notify() {}
}