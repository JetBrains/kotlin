// "Change 'A.component2' function return type to 'Unit'" "true"
// ERROR: An integer literal does not conform to the expected type kotlin.Unit
abstract class A {
    abstract fun component1(): Int
    fun component2(): Unit = 42
}

fun foo(a: A) {
    val (w: Int, x: Unit) = a<caret>
}