// FILE: A.java
// ANDROID_ANNOTATIONS

import kotlin.annotations.jvm.internal.*;

class A {
    public void emptyName(@ParameterName("") String first, @ParameterName("ok") int second) {
    }

    public void missingName(@ParameterName() String first) {
    }

    public void numberName(@ParameterName(42) String first) {
    }
}

