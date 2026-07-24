// IGNORE_BACKEND: JKLIB
// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// KT-85730 Reflection: incorrect nullability of raw type argument in new implementation
// KOTLIN_REFLECT_DUMP_MISMATCH

// FILE: A.java
public interface A<T> {
    public void foo(Inv<T> t);
}

// FILE: B.java
public interface B extends A {
    @Override public void foo(Inv t);
}

// FILE: main.kt
class Inv<T>(val t: T)

interface C {
    fun foo(t: Inv<*>)
}

interface D1 : C, B

interface D2 : C, B {
    override fun foo(t: Inv<*>)
}

interface D3 : B, C

interface D4 : B, C {
    override fun foo(t: Inv<*>)
}
