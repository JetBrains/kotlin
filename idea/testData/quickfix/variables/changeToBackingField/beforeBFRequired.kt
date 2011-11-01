// "Change reference to backing field" "true"
class A() {
    val a : Int
    {
        <caret>a = 1
    }
}