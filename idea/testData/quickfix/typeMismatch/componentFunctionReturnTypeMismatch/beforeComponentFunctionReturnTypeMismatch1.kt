// "Change 'A.component1' function return type to 'Int'" "true"
abstract class A {
    abstract fun component1()
    abstract fun component2(): Int
}

fun foo(a: A) {
    val (w: Int, x: Int) = a<caret>
}