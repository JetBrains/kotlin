// IS_APPLICABLE: false
class Foo() {
    var bar = 10
    fun get<T, U>(x: U, z: U, alpha: U): T {
        return x * 2
    }
    fun set(y: Int) {
        bar = y
    }
}
fun main(x: Int, y: Int) {
    var member = Foo()
    member.<caret>get<Int, Int>(5, 5, 5)
}