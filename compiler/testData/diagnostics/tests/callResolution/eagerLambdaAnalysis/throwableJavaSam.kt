// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP
// FULL_JDK
// WITH_STDLIB
// ISSUE: KT-7052
// LANGUAGE: +EagerLambdaAnalysis +InferThrowableTypeParameterToUpperBound

// FILE: com/intellij/openapi/util/Computable.java
package com.intellij.openapi.util;
import java.util.function.Supplier;
public interface Computable<V> extends Supplier<V> {
    V compute();
    
    @Override
    default V get() {
        return compute();
    }
}

// FILE: com/intellij/openapi/util/ThrowableComputable.java
package com.intellij.openapi.util;
public interface ThrowableComputable<T, E extends Throwable> {
    T compute() throws E;
}

// FILE: app/Application.java
package app;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.Computable;

public interface Application {
    // See Application.runReadAction
    public void runSomething(Runnable computable);
    public <T> T runSomething(Computable<T> computable);
    public <T, E extends Throwable> T runSomething(ThrowableComputable<T, E> computable) throws E;

    public <T> T runSomething2(Computable<T> computable);
    public <T, E extends Throwable> T runSomething2(ThrowableComputable<T, E> computable) throws E;

    // See ProgressManager.runProcessWithProgressSynchronously overloads
    public boolean runSomething3(Runnable runnable);
    public <T, E extends Throwable> T runSomething3(ThrowableComputable<T, E> computable) throws E;
}

// FILE: main.kt
import app.Application

fun myUnitFun() {}

fun main(app: Application) {
    app.runSomething { "" }
    app.runSomething { myUnitFun() }
    app.runSomething2 { "" }
    app.runSomething2 { myUnitFun() }
    app.runSomething3 { "" }
    app.runSomething3 { myUnitFun() }
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, stringLiteral */
