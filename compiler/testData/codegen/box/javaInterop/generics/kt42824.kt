// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: JS
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// IGNORE_BACKEND: NATIVE
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
