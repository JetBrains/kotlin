// FILE: Boo.java
public class Boo<P2, P3, P4> {
    static Foo test1() { return null; }
}

// FILE: Foo.kt
class Foo<P1 : Boo<P2, P3, P4>, P2 : Boo<P1, P3, P4>, P3 : Boo<P1, P2, P4>, P4 : Boo<P1, P2, P3>> {}

// FILE: main.kt
fun main() {
    val x = <!DEBUG_INFO_EXPRESSION_TYPE("Foo<kotlin.Any, kotlin.Any, kotlin.Any, kotlin.Any>..Foo<*, *, *, *>?!")!>Boo.test1()<!>
}
