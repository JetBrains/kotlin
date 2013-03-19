// "Change 'y' type to 'Int'" "true"
class A {
    fun component1() = 42
    fun component2() = 42
    fun component3() = 42
}

fun foo(a: A) {
    val (x: Int, y: String, z: Int) = a<caret>
}