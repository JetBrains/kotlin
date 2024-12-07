// SKIP_KT_DUMP
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// FULL_JDK

// FILE: Base.java
import org.jetbrains.annotations.NotNull;

public class Base<@NotNull T> {
    public T foo(T s) {}
}

// FILE: A.kt
class A : Base<Int>()

// FILE: B.kt
import java.util.SortedMap

abstract class B : SortedMap<Boolean, Boolean>
