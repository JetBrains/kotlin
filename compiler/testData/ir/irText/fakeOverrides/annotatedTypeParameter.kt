// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// FULL_JDK
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ISSUE: KT-65302

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
