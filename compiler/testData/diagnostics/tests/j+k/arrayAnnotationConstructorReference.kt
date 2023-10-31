// FIR_IDENTICAL
// WITH_REFLECT
// FILE: Ann.java

public @interface Ann {
    String[] value();
}

// FILE: Test.kt

import kotlin.reflect.*

fun test() {
    val x: KFunction1<Array<out String>, Ann> = ::<!CALLABLE_REFERENCE_TO_ANNOTATION_CONSTRUCTOR!>Ann<!>
}
