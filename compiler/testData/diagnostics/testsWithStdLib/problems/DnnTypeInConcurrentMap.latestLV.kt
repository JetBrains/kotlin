// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-87967
// FULL_JDK
// DISABLE_JAVA_FACADE
// LATEST_LV_DIFFERENCE
// FILE: CollectionFactory.java
import org.jetbrains.annotations.*;
import java.util.concurrent.*;

public class CollectionFactory {
  public static @NotNull <K, V> ConcurrentMap<@NotNull K, @NotNull V> createConcurrentHashMap() {
    return new ConcurrentHashMap<>();
  }
}

// FILE: Bar.kt
abstract class Bar<Element> {
    val map = CollectionFactory.createConcurrentHashMap<Element, String>()
    fun foo(element: Element) {
        map[<!ARGUMENT_TYPE_MISMATCH!>element<!>] = ""
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, flexibleType, functionDeclaration, javaFunction, nullableType,
propertyDeclaration, stringLiteral, typeParameter */
