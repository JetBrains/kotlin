// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FULL_JDK
// WITH_STDLIB
// DIAGNOSTICS: -UNUSED_PARAMETER -CAST_NEVER_SUCCEEDS -UNUSED_VARIABLE
// FILE: Simple.java

import java.util.*;
import java.util.function.Supplier;

public class Simple<K, V, C extends E, E extends Collection<V>> {
    public Simple(Map<K, C> backingMap, Supplier<? extends C> innerCollectionCreator) {
        // TODO
    }
    public final void add(K key, V value) {
        //TODO
    }
    public static class ListSimple<K, V> extends Simple<K, V, List<V>, List<V>> {
        public ListSimple(Map<K, List<V>> backingMap, Supplier<? extends List<V>> innerCollectionCreator) {
        super(backingMap, innerCollectionCreator);
    }
    }
}

// FILE: main.kt
import java.util.*

fun <K, V, C : E, E : Collection<V>, B: Simple<K, V, C, E>> Iterable<V>.groupByTo(destination: B, keySelector: (V) -> K) = null as B

enum class Format { Foo, Bar }

class Instance(val format: Format)

fun main(x: List<Instance>) {
    val doesntWork = x.groupByTo(
        Simple.ListSimple(EnumMap<Format, List<Instance>>(Format::class.java), ::LinkedList)
    ) { it.format } // Internal Error occurred while analyzing this expression
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, classReference, enumDeclaration, enumEntry, flexibleType,
funWithExtensionReceiver, functionDeclaration, functionalType, javaCallableReference, javaFunction, javaType,
lambdaLiteral, localProperty, nullableType, outProjection, primaryConstructor, propertyDeclaration, samConversion,
typeConstraint, typeParameter */
