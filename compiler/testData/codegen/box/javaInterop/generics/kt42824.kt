// TARGET_BACKEND: JVM

// FILE: DiagnosticFactory0.java

import org.jetbrains.annotations.NotNull;

public class DiagnosticFactory0<E> {
    @NotNull
    public SimpleDiagnostic<E> on(@NotNull E element) {
        return new SimpleDiagnostic<E>(element);
    }
}

// FILE: test.kt

class SimpleDiagnostic<E>(val element: E)
interface KtAnnotationEntry

fun foo(error: DiagnosticFactory0<in KtAnnotationEntry>, entry: KtAnnotationEntry) {
    error.on(entry) // used to be INAPPLICABLE_CANDIDATE
}

fun box() = "OK"
