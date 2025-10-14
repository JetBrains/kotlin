// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// ISSUE: KT-65207

// In new kotlin-reflect, when typealias type goes through KTypeSubstitutor, it looses "typealiasness".
// It's arguably a minor difference between K1 and new implementation
// SKIP_NEW_KOTLIN_REFLECT_COMPATIBILITY_CHECK

// FILE: Java1.java
import java.util.ArrayList;

public class Java1<T> {
    public T foo() {
        return null;
    }
    public ArrayList<Integer> bar() {
        return null;
    };
}

// FILE: 1.kt
abstract class C<R> : Java1<R>(), KotlinInterface<R>

interface KotlinInterface<T> {
    fun foo(): T
    fun bar(): ArrayList<Int>
}
