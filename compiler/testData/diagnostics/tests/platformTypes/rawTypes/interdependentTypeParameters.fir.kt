// FILE: Boo.java
public class Boo<N> {}

// FILE: Foo.java
public class Foo<P1 extends Boo<P2, P3, P4>, P2 extends Boo<P1, P3, P4>, P3 extends Boo<P1, P2, P4>, P4 extends Boo<P1, P2, P3>> {
    static Foo test1() { return null; }
}

// FILE: main.kt
fun main() {
    val x = <!DEBUG_INFO_EXPRESSION_TYPE("Foo<Boo<*>..Boo<*>?!, Boo<*>..Boo<*>?!, Boo<*>..Boo<*>?!, Boo<*>..Boo<*>?!>..Foo<*, *, *, *>?!")!>Foo.test1()<!>
}
