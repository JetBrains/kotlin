// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// FILE: spr/Exec.java
package spr;

import org.jetbrains.annotations.NotNull;

public interface Exec<E> {
    boolean run(@NotNull Model model, @NotNull Processor<? super E> params);
}

// FILE: spr/foo.kt
package spr

open class Processor<P> {
    fun process(t: P): Boolean {
        return true
    }
}

class Model

fun <C> context(p: Processor<in C>, exec: Exec<C>) {}

fun <M> materialize(): Processor<M> = TODO()

private fun foo(model: Model) {
    <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER("M")!>materialize<!>().<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER("T")!>apply<!> <!CANNOT_INFER_PARAMETER_TYPE!>{
        context(
            <!CANNOT_INFER_PARAMETER_TYPE!>this<!>,
            Exec { m, p -> p.process(m) } // Note: Builder inference
        )
    }<!>
}
