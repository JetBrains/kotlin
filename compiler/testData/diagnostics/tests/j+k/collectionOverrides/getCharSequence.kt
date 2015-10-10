// FILE: A.java
abstract public class A implements CharSequence {
    public char charAt(int x) { }
}

// FILE: C.java
abstract public class C implements CharSequence {
    public char get(int x) { }
}

// FILE: main.kt

abstract class B : A(), CharSequence {
    override operator fun get(index: Int) = '1'
}

fun main(a: A, b: B, c: C) {
    a[0]
    b[0]
    c[0]

    a.get(0)
    b.get(0)
    c.get(0)
}
