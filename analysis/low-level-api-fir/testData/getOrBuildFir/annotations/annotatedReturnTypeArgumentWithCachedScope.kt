// SKIP_WHEN_OUT_OF_CONTENT_ROOT

// FILE: org/jetbrains/annotations/Nls.java

package org.jetbrains.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.TYPE_USE})
public @interface Nls {}

// FILE: first.kt

fun test() {
    <expr_1>called()()</expr_1>
}

// FILE: second.kt

import org.jetbrains.annotations.Nls

var withoutAnno: () -> String = { "" }
var withAnno: () -> @Nls String = { "" }

<expr>fun unrelated(block: (String, @Nls String) -> Unit) {
    block(withoutAnno(), withoutAnno())
}</expr>

fun called(): () -> @Nls String = { "" }