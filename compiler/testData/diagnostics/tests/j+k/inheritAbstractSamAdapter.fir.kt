// FILE: A.java
public interface A {
    void foo(Runnable r);
}

// FILE: B.java
public interface B extends A {
    public void bar(Runnable r);
}

// FILE: test.kt
class C: B {
    override fun foo(r: Runnable?) {
    }

    override fun bar(r: Runnable?) {
    }
}
