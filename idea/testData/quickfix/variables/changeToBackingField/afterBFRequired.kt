// "Change reference to backing field" "true"
class A() {
    var a : Int
    set(v) {}
    init {
        <caret>$a = 1
    }
}