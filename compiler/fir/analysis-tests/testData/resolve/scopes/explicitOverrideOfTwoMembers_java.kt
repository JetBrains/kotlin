// SCOPE_DUMP: C:foo;x;y;getX, D:x;y;getX, E:x;getX

// FILE: lib.kt
interface A {
    fun foo(): Any
    val x: Int
    val y: Int
}

interface B {
    fun foo(): Any
    val x: String
    val y: Int
}

// FILE: C.java
public abstract class C {
    public int x;
    private int y;
}

// FILE: D.java
public abstract class D extends C implements A, B {
    public abstract Object foo();

    public abstract Object getX();
    public abstract int getY();
}

// FILE: E.java
public abstract class E implements A, B {
    public abstract Object foo();

    public abstract Object getX();
}

// FILE: main.kt

fun test(d: D) {
    val a = d.x
    val b = d.y
}
