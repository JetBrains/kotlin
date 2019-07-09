// FILE: A.java
// ANDROID_ANNOTATIONS
import kotlin.annotations.jvm.internal.*;
import kotlin.internal.*;

public class A {
    public void dollarName(@ParameterName("$") String host) {
    }

    public void numberName(@ParameterName("42") String field) {
    }
}

