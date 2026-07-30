// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-87967
// FULL_JDK
// JVM_TARGET: 1.8
// DISABLE_JAVA_FACADE
// FILE: CollectionFactory.java
import org.jetbrains.annotations.*;
import java.util.concurrent.*;

public class CollectionFactory {
  public static @NotNull <K, V> ConcurrentMap<@NotNull K, @NotNull V> createConcurrentSkipListMap() {
    return new ConcurrentSkipListMap<>();
  }
}

// FILE: Bar.kt

private val map = CollectionFactory.createConcurrentSkipListMap<Foo, Bar>()

private class Foo
private class Bar

private fun bar(): Bar? = null

private fun getOrCalculateDescriptor(foo: Foo): Bar? = map.computeIfAbsent(foo) { bar() }

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, inProjection, javaFunction, lambdaLiteral,
nullableType, outProjection, propertyDeclaration, samConversion */
