// SIBLING:
public class A() {
    fun bar(a: Int, b: Int): Int {
        return a + b
    }
}

fun foo(a: A, x: Int): Int {
    val t = 10
    val u = 20
    return <selection>a.bar(t - x, u + x)</selection>
}
