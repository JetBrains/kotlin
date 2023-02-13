// LL_FIR_DIVERGENCE
// Workaround for KT-56630
// LL_FIR_DIVERGENCE
// FILE: Boo.java
public class Boo<N> {}

// FILE: Foo.java
public class Foo<P1 extends Boo<P2, P3, P4>, P2 extends Boo<P1, P3, P4>, P3 extends Boo<P1, P2, P4>, P4 extends Boo<P1, P2, P3>> {
    static Foo test1() { return null; }
}

// FILE: main.kt
fun main() {
    val x = <!DEBUG_INFO_EXPRESSION_TYPE("Foo<*, *, *, *>..Foo<*, *, *, *>?!")!>Foo.test1()<!>
}
