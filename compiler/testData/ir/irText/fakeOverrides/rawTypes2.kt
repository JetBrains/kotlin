// IGNORE_BACKEND: JKLIB
// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// KT-85730 Reflection: incorrect nullability of raw type argument in new implementation
// KOTLIN_REFLECT_DUMP_MISMATCH

// FILE: A.java
public interface A<T extends Number> {
    public void foo(Inv2<Inv<T>, Inv> t);

    public void bar(Inv<Integer> t);
    public void baz(Inv<Inv<Integer>> t);
}

// FILE: B.java
public interface B<R> extends A {
}

// FILE: main.kt
class Inv<T>(val t: T)
class Inv2<A, B>(val a: A, val b: B)

interface C1 : A<Int> {
}

interface C2 : B<Int> {
}
