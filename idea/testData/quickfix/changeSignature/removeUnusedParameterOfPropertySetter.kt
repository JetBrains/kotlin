// "Remove parameter 'value'" "false"
// ACTION: Specify type explicitly
class Abacaba {
    var foo: String
        get() = ""
        set(<caret>value) {}
}