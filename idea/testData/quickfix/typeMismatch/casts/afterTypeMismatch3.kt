// "Cast expression 'a + a' to 'B'" "true"
trait A {
    fun plus(x: Any): A
}
trait B : A

fun foo(a: A): B {
    return (a + a) as B<caret>
}