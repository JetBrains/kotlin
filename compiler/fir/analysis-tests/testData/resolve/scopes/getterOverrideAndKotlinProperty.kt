// SCOPE_DUMP: A:getFoo, B:getFoo, C:getFoo, D:getFoo
// FILE: A.java
public interface A {
    String getFoo();
}

// FILE: B.kt
abstract class B : A {
    private val foo: Int = 1

    override fun getFoo(): String = "foo"
}

// FILE: C.java
public abstract class C extends B implements A {}

// FILE: main.kt
class D : C()

fun test(d: D) {
    d.foo.length
}
