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
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materialize<!>().<!DEBUG_INFO_MISSING_UNRESOLVED!>apply<!> {
        context(
            <!NO_THIS!>this<!>,
            Exec { m, p -> p.process(m) } // Note: Builder inference
        )
    }
}
