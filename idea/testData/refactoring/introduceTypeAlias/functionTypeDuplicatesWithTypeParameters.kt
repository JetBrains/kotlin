// NAME: S

class A<X>

// SIBLING:
fun foo() {
    class B<T>

    val t1: <caret>(B<Int>) -> ((B<Int>) -> Boolean)
    val t2: (B<Int>) -> ((B<Int>) -> Boolean)
    val t3: ((B<Int>) -> B<Int>) -> Boolean
    val t4: Function1<B<Int>, Function1<B<Int>, Boolean>>
    val t5: (String) -> ((String) -> Boolean)
    val t6: (Int) -> ((String) -> Boolean)
    val t7: (A<B<String>>) -> ((A<B<String>>) -> Boolean)
}