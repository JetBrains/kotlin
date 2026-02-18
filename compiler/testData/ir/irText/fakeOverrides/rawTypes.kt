// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FILE: A.java
public interface A<T extends Number> {
    public void foo1(Inv<Inv<T>> t);
    public void foo2(Inv<Inv<T>> t);
    public void foo3(Inv2<Inv<T>, Inv> t);
    public void baz1(Inv<Inv<T>> t);
    public void baz2(Inv<Inv<T>> t);
}

// FILE: B.java
public interface B<R> extends A {
    public void bar1(Inv<Inv<R>> t);
    public void bar2(Inv<Inv> t);
    public void bar3(Inv t);
    @Override public void baz1(Inv n);
    @Override public void baz2(Inv n);
}

// FILE: main.kt
class Inv<T>(val t: T)
class Inv2<A, B>(val a: A, val b: B)

interface C1 : B<Int> {
    override fun foo2(t: Inv<*>?)
    override fun baz2(t: Inv<*>?)
}

interface C2 : B<Int> {
    override fun bar1(t: Inv<Inv<Int>>)
    override fun bar2(t: Inv<Inv<*>>)
    override fun bar3(t: Inv<*>)
}
