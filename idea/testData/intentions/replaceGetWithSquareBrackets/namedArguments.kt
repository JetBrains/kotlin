// IS_APPLICABLE: false
class Foo() {
    var bar = 10
    fun get(x: U, z: U, alpha: U): Int {
        return x * 2
    }
    fun set(y: Int) {
        bar = y
    }
}
fun main(x: Int, y: Int) {
    var member = Foo()
    member.<caret>get(x = 1, z = 2, alpha = 3)
}