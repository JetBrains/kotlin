// !WITH_NEW_INFERENCE

// FILE: J.java

import kotlin.jvm.functions.Function1;

public interface J extends Function1<Integer, Void> {
}

// FILE: 1.kt

fun useJ(j: J) {
    j(42)
}

fun jj() {
    useJ(<!ARGUMENT_TYPE_MISMATCH!>{}<!>)
}
