// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java2.java
public interface Java2<T> extends A<T> { }


// FILE: 1.kt
interface A<T> {
    fun bar(o: T);
}
