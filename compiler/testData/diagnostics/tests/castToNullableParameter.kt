// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND

// FILE: Supplier.java
public interface Supplier<M> {
    M get();
}

// FILE: Option.java
public abstract class Option<T> {
    abstract T getOrElse(T other);
    abstract T getOrElse(Supplier<? extends T> supplier);
}

// FILE: Kotlin.kt
fun <D: Any> test(o: Option<D>) {
    o.getOrElse(null as D?)
    o.<!OVERLOAD_RESOLUTION_AMBIGUITY!>getOrElse<!>(null)
}

/* GENERATED_FIR_TAGS: asExpression, checkNotNullCall, classDeclaration, functionDeclaration, interfaceDeclaration,
nullableType, typeParameter */
