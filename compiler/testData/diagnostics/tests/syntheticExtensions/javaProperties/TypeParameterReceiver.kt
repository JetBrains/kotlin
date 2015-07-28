// FILE: KotlinFile.kt

fun <T: B> foo(t: T) {
    t.setSomething(t.getSomething() + 1)
    t.something++
}

// FILE: A.java
public interface A {
    int getSomething();
}

// FILE: B.java
public interface B extends A {
    void setSomething(int value);
}
