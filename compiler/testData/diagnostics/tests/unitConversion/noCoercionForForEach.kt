// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK
// WITH_STDLIB

// FILE: Processor.java
public interface Processor<T> {
    boolean process(T t);
}

// FILE: main.kt
import java.util.function.Consumer

interface Query<R> : Iterable<R> {
    fun forEach(consumer: Processor<in R>): Boolean
    override fun forEach(consumer: Consumer<in R>)
}

fun foo(query: Query<String>, processor: (String) -> Boolean) {
    query.forEach(processor).not()
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, integerLiteral, javaFunction, lambdaLiteral, localProperty,
propertyDeclaration, samConversion, starProjection, stringLiteral */
