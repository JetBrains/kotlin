// FILE: J.java
public interface J {
    @Deprecated
    public void foo();

}
// FILE: J2.java
public interface J2 extends J {
    @Override
    public void foo();
}

// FILE: main.kt

interface A : J {
    override fun foo()
}

fun main(j: J, j2: J2, a: A) {
    j.<!DEPRECATION!>foo<!>()
    j2.foo()
    a.foo()
}
