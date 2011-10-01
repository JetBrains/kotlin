// "Make variable mutable" "true"
class A() {
    var a: Int = 0
        <caret>set(v: Int) {}
}
