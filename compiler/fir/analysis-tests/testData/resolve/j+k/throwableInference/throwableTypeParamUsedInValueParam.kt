// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +InferThrowableTypeParameterToUpperBound
// FULL_JDK
// ISSUE: KT-82961

// FILE: ThrowableCallable.java
public interface ThrowableCallable<E extends Throwable> {
    void call() throws E;
}

// FILE: JavaHelper.java
import java.util.function.Consumer;

public class JavaHelper {
    public static <E extends Throwable> void handle(Consumer<E> handler) {
    }

    public static <E extends Throwable> void process(E exception) {
    }

    public static <E extends Throwable> void runWithHandler(ThrowableCallable<E> callable, Consumer<E> errorHandler) {
    }
}

// FILE: test.kt
fun test() {
    JavaHelper.<!CANNOT_INFER_PARAMETER_TYPE!>handle<!> { <!CANNOT_INFER_PARAMETER_TYPE!>e<!> -> }
    JavaHelper.process(RuntimeException())
    JavaHelper.process(null)

    JavaHelper.<!CANNOT_INFER_PARAMETER_TYPE!>runWithHandler<!>({}, { <!CANNOT_INFER_PARAMETER_TYPE!>e<!> -> })
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType, lambdaLiteral, samConversion */
