// FILE: A.java
abstract public class A implements CharSequence {
    public char charAt(int x) { }
    public int length() { return 1; }
}

// FILE: C.java
abstract public class C implements CharSequence {
    public char get(int x) { }
    public int length() { return 1; }
}

// FILE: main.kt

abstract class B : A(), CharSequence {
    override operator fun get(index: Int) = '1'
    override val length: Int get() = 1
}

fun main(a: A, b: B, c: C) {
    a[0]
    b[0]
    c[0]

    a.get(0)
    b.get(0)
    c.get(0)

    a.length + b.length + c.length
}
