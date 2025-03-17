// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-63676
// LANGUAGE: +AllowDnnTypeOverridingFlexibleType

// FILE: J.java
public interface J<T> {
    void simple(T t);
    void box(Box<T> box);
}

// FILE: test.kt
class Box<T>

class K<T> : J<T> {
    override fun simple(t: T & Any) {}
    override fun box(box: Box<T & Any>) {}
}
