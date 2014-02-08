// IS_APPLICABLE: false
class Foo() {
    var bar = 10
    fun get(x: Int, z: Int, alpha: Int): Int {
        return x * 2
    }
    fun set(y: Int) {
        bar = y
    }
}

fun main(x: Int, y: Int) {
    var member = Foo()
    member.get<caret>(5, 5
}