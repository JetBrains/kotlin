// "Change reference to backing field" "true"
class A() {
    var a : Int
    set(v) {}
    {
        <caret>a = 1
    }
}